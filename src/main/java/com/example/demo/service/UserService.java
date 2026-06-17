package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.entity.MerchantDetail;
import com.example.demo.entity.Order;
import com.example.demo.entity.RiderDetail;
import com.example.demo.entity.User;
import com.example.demo.entity.UserAddress;
import com.example.demo.exception.BusinessException;
import com.example.demo.repository.MerchantDetailRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.RiderDetailRepository;
import com.example.demo.repository.UserAddressRepository;
import com.example.demo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 用户服务 —— 登录注册 + 个人信息管理 + 地址管理
 * 所有数据库操作均通过 JPA 参数化查询执行，防止SQL注入
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final UserAddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final MerchantDetailRepository merchantDetailRepository;
    private final RiderDetailRepository riderDetailRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       UserAddressRepository addressRepository,
                       OrderRepository orderRepository,
                       MerchantDetailRepository merchantDetailRepository,
                       RiderDetailRepository riderDetailRepository,
                       BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.orderRepository = orderRepository;
        this.merchantDetailRepository = merchantDetailRepository;
        this.riderDetailRepository = riderDetailRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ==================== 注册与登录 ====================

    /**
     * 用户注册（BCrypt加密存储密码）
     * 商家和骑手注册后需管理员审核（status=2），普通用户直接启用（status=1）
     */
    @Transactional
    public Map<String, Object> register(RegisterRequest request) {
        String username = request.getUsername();

        if (userRepository.existsByUsername(username)) {
            log.warn("注册失败：用户名 [{}] 已存在", username);
            throw new BusinessException(409, "用户名已存在，请更换后重试");
        }

        String role = request.getRole() != null ? request.getRole() : "user";

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));  // BCrypt加密
        user.setPhone(request.getPhone());
        user.setRole(role);

        // 商家和骑手注册后需管理员审核，普通用户直接启用
        if ("merchant".equals(role) || "rider".equals(role)) {
            user.setStatus(2); // 待审核
        } else {
            user.setStatus(1); // 直接启用
        }

        user = userRepository.save(user);

        // 自动创建商家/骑手详情记录（enabled=false，待审核）
        if ("merchant".equals(role)) {
            MerchantDetail detail = new MerchantDetail();
            detail.setUserId(user.getId());
            detail.setShopName(username + "的店铺");
            detail.setEnabled(false);
            merchantDetailRepository.save(detail);
        } else if ("rider".equals(role)) {
            RiderDetail detail = new RiderDetail(user.getId());
            detail.setRealName(username);
            detail.setEnabled(false);
            riderDetailRepository.save(detail);
        }
        log.info("用户注册成功：id={}, username={}, role={}, status={}",
                user.getId(), user.getUsername(), role, user.getStatus());

        return buildUserResult(user);
    }

    /**
     * 用户登录（BCrypt密码验证）
     */
    public Map<String, Object> login(LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            log.warn("登录失败：用户名 [{}] 不存在", username);
            throw new BusinessException(401, "用户名或密码错误");
        }

        User user = userOpt.get();

        // 检查账户状态（仅禁止已禁用的账户登录）
        if (user.getStatus() == null || user.getStatus() == 0) {
            log.warn("登录失败：用户 [{}] 已被禁用, status={}", username, user.getStatus());
            throw new BusinessException(403, "账户已被禁用，请联系管理员");
        }

        // BCrypt密码验证
        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("登录失败：用户 [{}] 密码错误", username);
            throw new BusinessException(401, "用户名或密码错误");
        }

        log.info("用户登录成功：id={}, username={}", user.getId(), user.getUsername());
        return buildUserResult(user);
    }

    // ==================== 个人信息管理 ====================

    /**
     * 获取当前用户个人信息
     */
    public Map<String, Object> getProfile(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        return buildUserResult(user);
    }

    /**
     * 更新个人信息（用户名、手机号）
     */
    @Transactional
    public Map<String, Object> updateProfile(Integer userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        // 更新用户名（需校验唯一性）
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new BusinessException(409, "该用户名已被使用，请更换后重试");
            }
            user.setUsername(request.getUsername());
        }

        // 更新手机号
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        user = userRepository.save(user);

        log.info("用户信息更新成功：id={}", userId);
        return buildUserResult(user);
    }

    /**
     * 修改密码（需验证旧密码）
     */
    @Transactional
    public void changePassword(Integer userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        // 验证旧密码
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(400, "旧密码错误");
        }

        // 新密码不能与旧密码相同
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException(400, "新密码不能与旧密码相同");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("密码修改成功：id={}", userId);
    }

    // ==================== 地址管理 ====================

    /**
     * 查询当前用户的所有地址
     */
    public List<UserAddress> getAddresses(Integer userId) {
        return addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
    }

    /**
     * 新增地址
     */
    @Transactional
    public UserAddress addAddress(Integer userId, AddressRequest request) {
        UserAddress address = new UserAddress();
        address.setUserId(userId);
        address.setContactName(request.getContactName());
        address.setPhone(request.getPhone());
        address.setAddress(request.getAddress());

        // 处理默认地址
        boolean setDefault = request.getIsDefault() != null && request.getIsDefault();
        if (setDefault) {
            addressRepository.clearDefaultByUserId(userId);
            address.setIsDefault(true);
        } else {
            // 如果是第一个地址，自动设为默认
            long count = addressRepository.countByUserId(userId);
            address.setIsDefault(count == 0);
        }

        address = addressRepository.save(address);
        log.info("地址新增成功：id={}, userId={}", address.getId(), userId);
        return address;
    }

    /**
     * 编辑地址（含订单同步）
     */
    @Transactional
    public UserAddress updateAddress(Integer userId, Integer addressId, AddressRequest request) {
        UserAddress address = validateAddressOwnership(userId, addressId);

        // 捕获旧值用于订单同步
        String oldAddress = address.getAddress();
        String oldPhone = address.getPhone();
        String oldContactName = address.getContactName();

        address.setContactName(request.getContactName());
        address.setPhone(request.getPhone());
        address.setAddress(request.getAddress());

        // 处理默认地址切换
        boolean setDefault = request.getIsDefault() != null && request.getIsDefault();
        if (setDefault && !Boolean.TRUE.equals(address.getIsDefault())) {
            addressRepository.clearDefaultByUserId(userId);
            address.setIsDefault(true);
        }

        address = addressRepository.save(address);
        log.info("地址更新成功：id={}, userId={}", addressId, userId);

        // 同步更新关联订单（pending_payment / pending 状态）
        syncOrdersAfterAddressUpdate(userId, oldAddress, oldPhone, oldContactName,
                request.getAddress(), request.getPhone(), request.getContactName());

        return address;
    }

    /**
     * 删除地址
     */
    @Transactional
    public void deleteAddress(Integer userId, Integer addressId) {
        UserAddress address = validateAddressOwnership(userId, addressId);
        // 同步更新关联订单中的地址信息
        syncOrdersAfterAddressDelete(userId, address);
        addressRepository.delete(address);
        log.info("地址删除成功：id={}, userId={}", addressId, userId);
    }

    /**
     * 批量删除地址（含订单同步）
     */
    @Transactional
    public void deleteAddressesBatch(Integer userId, List<Integer> addressIds) {
        if (addressIds == null || addressIds.isEmpty()) {
            throw new BusinessException(400, "请选择要删除的地址");
        }

        for (Integer addressId : addressIds) {
            UserAddress address = validateAddressOwnership(userId, addressId);
            // 删除前同步关联订单
            syncOrdersAfterAddressDelete(userId, address);
            addressRepository.delete(address);
        }

        log.info("批量删除地址成功：userId={}, count={}", userId, addressIds.size());
    }

    /**
     * 设置默认地址
     */
    @Transactional
    public void setDefaultAddress(Integer userId, Integer addressId) {
        validateAddressOwnership(userId, addressId);
        addressRepository.clearDefaultByUserId(userId);

        UserAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new BusinessException(404, "地址不存在"));
        address.setIsDefault(true);
        addressRepository.save(address);

        log.info("设置默认地址成功：id={}, userId={}", addressId, userId);
    }

    // ==================== 头像管理 ====================

    private static final String AVATAR_DIR = "uploads/avatars";

    /**
     * 保存用户头像到磁盘，返回访问URL
     * 不修改数据库结构，仅通过文件系统存储
     */
    public String saveAvatar(Integer userId, org.springframework.web.multipart.MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "上传文件不能为空");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.matches("image/(jpeg|png|gif|webp)")) {
            throw new BusinessException(400, "仅支持 JPG/PNG/GIF/WebP 格式图片");
        }

        try {
            Path avatarDir = Paths.get(AVATAR_DIR).toAbsolutePath().normalize();
            Files.createDirectories(avatarDir);

            // 清除旧头像文件
            String[] oldExts = {".jpg", ".jpeg", ".png", ".gif", ".webp"};
            for (String ext : oldExts) {
                try {
                    Files.deleteIfExists(avatarDir.resolve("user_" + userId + ext));
                } catch (IOException ignored) {
                }
            }

            // 确定新文件扩展名
            String originalName = file.getOriginalFilename();
            String extension = ".jpg";
            if (originalName != null && originalName.contains(".")) {
                String ext = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
                if (ext.matches("\\.(jpg|jpeg|png|gif|webp)")) {
                    extension = ext;
                }
            }

            // 保存新头像
            String filename = "user_" + userId + extension;
            Path targetPath = avatarDir.resolve(filename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String url = "/uploads/avatars/" + filename;
            log.info("头像上传成功：userId={}, url={}", userId, url);
            return url;
        } catch (IOException e) {
            log.error("头像保存失败：userId={}", userId, e);
            throw new BusinessException(500, "头像保存失败，请稍后重试");
        }
    }

    // ==================== 账户删除 ====================

    /**
     * 删除账户（软删除：将状态设为0-禁用）
     * 用户可自行删除自己的账户
     */
    @Transactional
    public void deleteAccount(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        // 软删除：将状态设为0（禁用），保留数据
        user.setStatus(0);
        userRepository.save(user);

        log.info("账户已删除（软删除）：id={}, username={}", userId, user.getUsername());
    }

    // ==================== 内部工具方法 ====================

    /**
     * 地址修改后同步关联的未完成订单
     * 仅同步 pending_payment（待支付）和 pending（待接单）状态
     * 已备餐/配送中/完成的订单不回溯修改（数据一致性）
     */
    private void syncOrdersAfterAddressUpdate(Integer userId,
                                               String oldAddress, String oldPhone, String oldContactName,
                                               String newAddress, String newPhone, String newContactName) {
        List<Order> pendingOrders = orderRepository.findByUserIdAndOrderStatusOrderByCreatedAtDesc(
                userId, "pending_payment");
        pendingOrders.addAll(orderRepository.findByUserIdAndOrderStatusOrderByCreatedAtDesc(
                userId, "pending"));

        int syncedCount = 0;
        for (Order order : pendingOrders) {
            boolean matched = false;

            // 通过旧地址数据匹配（地址快照精确匹配）
            if (oldAddress != null && oldAddress.equals(order.getDeliveryAddress())) {
                order.setDeliveryAddress(newAddress);
                matched = true;
            }
            if (oldPhone != null && oldPhone.equals(order.getContactPhone())) {
                order.setContactPhone(newPhone);
                matched = true;
            }
            if (oldContactName != null && oldContactName.equals(order.getContactName())) {
                order.setContactName(newContactName);
                matched = true;
            }

            if (matched) {
                orderRepository.save(order);
                syncedCount++;
            }
        }

        if (syncedCount > 0) {
            log.info("地址修改后同步 {} 个未完成订单：userId={}, addressId updated", syncedCount, userId);
        }
    }

    /**
     * 地址删除后：将关联未完成订单的地址标记为"已删除"
     * 保留原地址数据以确保订单可追溯，仅追加标记
     */
    private void syncOrdersAfterAddressDelete(Integer userId, UserAddress deletedAddress) {
        List<Order> pendingOrders = orderRepository.findByUserIdAndOrderStatusOrderByCreatedAtDesc(
                userId, "pending_payment");
        pendingOrders.addAll(orderRepository.findByUserIdAndOrderStatusOrderByCreatedAtDesc(
                userId, "pending"));

        int syncedCount = 0;
        for (Order order : pendingOrders) {
            if (deletedAddress.getAddress() != null
                    && deletedAddress.getAddress().equals(order.getDeliveryAddress())) {
                // 追加删除标记，保留原始地址信息
                if (order.getNote() == null || !order.getNote().contains("⚠️地址已删除")) {
                    String remark = (order.getNote() != null ? order.getNote() + "；" : "")
                            + "⚠️地址已删除（原地址：" + order.getDeliveryAddress() + "）";
                    order.setNote(remark);
                    syncedCount++;
                }
                orderRepository.save(order);
            }
        }

        if (syncedCount > 0) {
            log.info("地址删除后标记 {} 个未完成订单：userId={}", syncedCount, userId);
        }
    }

    /**
     * 校验地址是否属于当前用户（防止越权操作）
     */
    private UserAddress validateAddressOwnership(Integer userId, Integer addressId) {
        UserAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new BusinessException(404, "地址不存在"));
        if (!address.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该地址");
        }
        return address;
    }

    /**
     * 构建用户信息Map（排除密码，含头像URL）
     */
    private Map<String, Object> buildUserResult(User user) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("phone", user.getPhone());
        result.put("role", user.getRole());
        result.put("status", user.getStatus());
        result.put("createdAt", user.getCreatedAt());

        // 头像URL：检查磁盘是否存在头像文件
        result.put("avatarUrl", resolveAvatarUrl(user.getId()));

        return result;
    }

    /**
     * 根据用户ID解析头像URL（无需数据库字段）
     */
    private String resolveAvatarUrl(Integer userId) {
        String[] extensions = {".jpg", ".jpeg", ".png", ".gif", ".webp"};
        Path avatarDir = Paths.get(AVATAR_DIR).toAbsolutePath().normalize();
        for (String ext : extensions) {
            Path avatarFile = avatarDir.resolve("user_" + userId + ext);
            if (Files.exists(avatarFile)) {
                return "/uploads/avatars/user_" + userId + ext;
            }
        }
        return null;
    }
}
