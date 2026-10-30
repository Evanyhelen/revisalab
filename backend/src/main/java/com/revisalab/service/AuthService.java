package com.revisalab.service;

import com.revisalab.dto.request.SignupRequest;
import com.revisalab.mapper.UserMapper;
import com.revisalab.model.Role;
import com.revisalab.model.User;
import com.revisalab.enums.ERole;
import com.revisalab.repository.RoleRepository;
import com.revisalab.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Serviço de Registro de usuários.
 * Responsabilidades:
 * - Registrar novos usuários (signup)
 * - Validar disponibilidade de email
 * - Criptografar senha e atribuir a role padrão
 *
 * A autenticação em si (login) é feita diretamente no {@code AuthController},
 * que orquestra o {@code AuthenticationManager} e a geração do JWT.
 */
@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Autowired
    public AuthService(UserRepository userRepository,
                      RoleRepository roleRepository,
                      PasswordEncoder passwordEncoder,
                      UserMapper userMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    /**
     * Registra um novo usuário no sistema.
     *
     * @param signupRequest Dados do cadastro (nome, email, senha e, opcionalmente, curso/semestre/instituição)
     * @return Usuário criado
     * @throws IllegalArgumentException se email já existe
     */
    public User register(SignupRequest signupRequest) {
        // Validação de email duplicado
        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            throw new IllegalArgumentException("Email já está em uso: " + signupRequest.getEmail());
        }

        // Criar novo usuário usando UserMapper
        User user = userMapper.toEntity(signupRequest);
        user.setPassword(passwordEncoder.encode(signupRequest.getPassword())); // Criptografar senha

        // Atribuir role padrão
        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
            .orElseThrow(() -> new RuntimeException("Role ROLE_USER não encontrado"));
        roles.add(userRole);
        user.setRoles(roles);

        return userRepository.save(user);
    }

    /**
     * Verifica se um email está disponível para registro.
     *
     * @param email Email a verificar
     * @return true se disponível, false se já existe
     */
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    /**
     * Obtém usuário por email.
     *
     * @param email Email do usuário
     * @return Usuário encontrado
     * @throws IllegalArgumentException se não encontrado
     */
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + email));
    }
}
