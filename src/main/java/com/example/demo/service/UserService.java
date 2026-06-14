package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.entity.User;
import com.example.demo.entity.UserAddress;
import com.example.demo.exception.BusinessException;
import com.example.demo.repository.UserAddressRepository;
import com.example.demo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 用户服务 —— 登录注册 + 个人信息管理 + 地址管理
 * 所有数据库操作均通过 JPA 参数化查询执行，防止SQL注入
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final UserAddressRepository addressRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       UserAddressRepository addressRepository,
                       BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
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

        // 检查账户状态
        if (user.getStatus() == null || user.getStatus() == 0) {
            log.warn("登录失败：用户 [{}] 已被禁用, status={}", username, user.getStatus());
            throw new BusinessException(403, "账户已被禁用，请联系管理员");
        }

        if (user.getStatus() == 2) {
            log.warn("登录失败：用户 [{}] 待审核中, status={}", username, user.getStatus());
            throw new BusinessException(403, "您的账号正在审核中，请耐心等待管理员审核通过");
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
     * 更新个人信息（手机号）
     */
    @Transactional
    public Map<String, Object> updateProfile(Integer userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        user.setPhone(request.getPhone());
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
     * 编辑地址
     */
    @Transactional
    public UserAddress updateAddress(Integer userId, Integer addressId, AddressRequest request) {
        UserAddress address = validateAddressOwnership(userId, addressId);

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
        return address;
    }

    /**
     * 删除地址
     */
    @Transactional
    public void deleteAddress(Integer userId, Integer addressId) {
        UserAddress address = validateAddressOwnership(userId, addressId);
        addressRepository.delete(address);
        log.info("地址删除成功：id={}, userId={}", addressId, userId);
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

    // ==================== 内部工具方法 ====================

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
     * 构建用户信息Map（排除密码）
     */
    private Map<String, Object> buildUserResult(User user) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("phone", user.getPhone());
        result.put("role", user.getRole());
        result.put("status", user.getStatus());
        result.put("createdAt", user.getCreatedAt());
        return result;
    }
}
