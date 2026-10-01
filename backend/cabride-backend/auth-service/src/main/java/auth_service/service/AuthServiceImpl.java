package auth_service.service;


import auth_service.dto.response.AuthResponse;
import auth_service.dto.LoginRequest;
import auth_service.dto.RegisterRequest;
import auth_service.dto.response.UserResponse;
import auth_service.entity.User;
import auth_service.event.UserCreatedEvent;
import auth_service.event.UserEventPublisher;
import auth_service.exception.InvalidCredentialsException;
import auth_service.exception.ResourceAlreadyExistsException;
import auth_service.mapper.UserMapper;
import auth_service.repository.UserRepository;
import auth_service.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    private final UserEventPublisher userEventPublisher;

    @Override
    public UserResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException("Email already exists.");
        }

      User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setEmailVerified(false);
        user.setAccountNonLocked(true);
        user.setFailedAttempts(0);

        User savedUser=userRepository.save(user);

        UserCreatedEvent event=UserCreatedEvent.builder()
                .userId(savedUser.getId())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .phoneNumber(savedUser.getPhoneNumber())
                .build();

        userEventPublisher.publishedUserCreated(event);

        return UserResponse.builder()
                .id(savedUser.getId())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .email(savedUser.getEmail())
                .phoneNumber(savedUser.getPhoneNumber())
                .role(savedUser.getRole())
                .build();


    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user=userRepository.findByEmail(request.getEmail())
                .orElseThrow(()-> new InvalidCredentialsException("Invalid email or password"));

        if(!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )){
            throw new InvalidCredentialsException("Invalid email or password");        }


        String token=jwtService.generateToken(user);

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(3600000L)
                .build();

    }
}
