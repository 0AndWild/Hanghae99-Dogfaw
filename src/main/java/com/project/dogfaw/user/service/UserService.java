package com.project.dogfaw.user.service;

import com.project.dogfaw.acceptance.model.Acceptance;
import com.project.dogfaw.acceptance.repository.AcceptanceRepository;
import com.project.dogfaw.apply.model.UserApplication;
import com.project.dogfaw.apply.repository.UserApplicationRepository;
import com.project.dogfaw.common.CommonService;
import com.project.dogfaw.common.exception.CustomException;
import com.project.dogfaw.common.exception.ErrorCode;
import com.project.dogfaw.common.validator.UserValidator;
import com.project.dogfaw.post.model.Post;
import com.project.dogfaw.security.jwt.JwtReturn;
import com.project.dogfaw.security.jwt.JwtTokenProvider;
import com.project.dogfaw.security.jwt.TokenDto;
import com.project.dogfaw.security.jwt.TokenRequestDto;
import com.project.dogfaw.user.dto.LoginDto;
import com.project.dogfaw.user.dto.SignupRequestDto;
import com.project.dogfaw.user.dto.StackDto;
import com.project.dogfaw.user.model.RefreshToken;
import com.project.dogfaw.user.model.Stack;
import com.project.dogfaw.user.model.User;
import com.project.dogfaw.user.model.UserRoleEnum;
import com.project.dogfaw.user.repository.RefreshTokenRepository;
import com.project.dogfaw.user.repository.StackRepository;
import com.project.dogfaw.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.parameters.P;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final StackRepository stackRepository;
    private final CommonService commonService;
    private final UserApplicationRepository userApplicationRepository;
    private final AcceptanceRepository acceptanceRepository;


    // ?????? ????????????
    @Transactional
    public TokenDto register(SignupRequestDto requestDto) {

        // ?????? ????????? ?????? ??????
        String username = requestDto.getUsername();
        if (userRepository.existsByUsername(username)) {
            throw new CustomException(ErrorCode.SIGNUP_MEMBERID_DUPLICATE_CHECK);
        }

        // ????????? ?????? ??????
        String nickname = requestDto.getNickname();
        if (userRepository.existsByNickname(nickname)) {
            throw new CustomException(ErrorCode.SIGNUP_NICKNAME_DUPLICATE_CHECK);
        }

        // ?????? ???????????? ?????????
        String password = passwordEncoder.encode(requestDto.getPassword());

        // ????????? ??????
//        UserValidator.validateInputUsername(requestDto);
        UserValidator.validateInputPassword(requestDto);


        User user = userRepository.save(
                User.builder()
                        .username(requestDto.getUsername())
                        .password(password)
                        .nickname(requestDto.getNickname())
                        .role(UserRoleEnum.USER)
                        .build()
        );

        List<Stack> stack = stackRepository.saveAll(tostackByUserId(requestDto.getStacks(),user));

        user.updateStack(stack);

        TokenDto tokenDto = jwtTokenProvider.createToken(user);

        RefreshToken refreshToken = new RefreshToken(user.getUsername(), tokenDto.getRefreshToken());
        refreshTokenRepository.save(refreshToken);

        return tokenDto;
    }


    // ?????????
    @Transactional
    public Map<String, Object> login(LoginDto loginDto) {
        UserValidator.validateUsernameEmpty(loginDto);
        UserValidator.validatePasswordEmpty(loginDto);

        User user = userRepository.findByUsername(loginDto.getUsername()).orElseThrow(
                () -> new CustomException(ErrorCode.LOGIN_NOT_FOUNT_MEMBERID)
        );

        if (!passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.LOGIN_PASSWORD_NOT_MATCH);
        }

        Long userId = user.getId();

        TokenDto tokenDto = jwtTokenProvider.createToken(user);

        RefreshToken refreshToken = new RefreshToken(loginDto.getUsername(), tokenDto.getRefreshToken());
        refreshTokenRepository.save(refreshToken);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("token", tokenDto);

        return data;
    }

    // ????????????
    @Transactional
    public void deleteRefreshToken(TokenRequestDto tokenRequestDto) {
        User user = userRepository.findById(tokenRequestDto.getUserId()).orElseThrow(
                () -> new CustomException(ErrorCode.NOT_FOUND_USER_INFO)
        );
        String username = user.getUsername();

        RefreshToken refreshToken = refreshTokenRepository.findByRefreshKey(username).orElseThrow(
                () -> new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND)
        );
        refreshTokenRepository.deleteById(refreshToken.getRefreshKey());
    }
    

    // reissue(Token ?????????)
    @Transactional
    public TokenDto reissue(TokenRequestDto tokenRequestDto) {
        log.info("Refresh Token : " + tokenRequestDto.getRefreshToken());

        UserValidator.validateRefreshTokenReissue(tokenRequestDto);

        // RefreshToken ???????????? ??????
        if (jwtTokenProvider.validateToken(tokenRequestDto.getRefreshToken()) != JwtReturn.SUCCESS) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        User user = userRepository.findById(tokenRequestDto.getUserId()).orElseThrow(
                () -> new CustomException(ErrorCode.NOT_FOUND_USER_INFO)
        );

        // RefreshToken DB??? ?????? ??????
        RefreshToken refreshToken = refreshTokenRepository.findByRefreshKey(user.getUsername()).orElseThrow(
                () -> new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND)
        );

        // RefreshToken ???????????? ?????? ??????
        if (!refreshToken.getRefreshValue().equals(tokenRequestDto.getRefreshToken())) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_NOT_MATCH);
        }

        // Access Token, Refresh Token ?????????
        TokenDto tokenDto = jwtTokenProvider.createToken(user);
        RefreshToken updateRefreshToken = refreshToken.updateValue(tokenDto.getRefreshToken());
        refreshTokenRepository.save(updateRefreshToken);

        return tokenDto;
    }

    // ????????? ????????? ?????? ?????? ??????
    public TokenDto SignupUserCheck(Long kakaoId) {

        User loginUser = userRepository.findByKakaoId(kakaoId).orElse(null);

        TokenDto tokenDto = jwtTokenProvider.createToken(loginUser);
        RefreshToken refreshToken = new RefreshToken(loginUser.getUsername(), tokenDto.getRefreshToken());
        refreshTokenRepository.save(refreshToken);
        return TokenDto.builder()
                .accessToken(tokenDto.getAccessToken())
                .refreshToken(refreshToken.getRefreshValue())
                .build();
    }

    //?????? ?????? ??????
    @Transactional
    public TokenDto addInfo(SignupRequestDto requestDto, User user1) {
        // ????????? ?????? ??????
        String nickname = requestDto.getNickname();
        if (userRepository.existsByNickname(nickname)) {
            throw new CustomException(ErrorCode.SIGNUP_NICKNAME_DUPLICATE_CHECK);
        }

        User user = userRepository.findById(user1.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.LOGIN_NOT_FOUNT_MEMBERID));

        user.updateNickname(requestDto);
        List<Stack> stack = stackRepository.saveAll(tostackByUserId(requestDto.getStacks(),user));
        user.updateStack(stack);

        TokenDto tokenDto = jwtTokenProvider.createToken(user);

        RefreshToken refreshToken = new RefreshToken(user.getUsername(), tokenDto.getRefreshToken());
        refreshTokenRepository.save(refreshToken);
        return tokenDto;
    }

    // ?????? ?????? ????????? (??????????????? ????????? ??????????????? ??????)
    @Transactional
    public void deleteUser() {
        User foundUser = commonService.getUser();

        if (foundUser != null) {
            String username = "deleteUser_"+foundUser.getId();
            String password = passwordEncoder.encode(UUID.randomUUID().toString());
            String nickname = "????????????";

            //??????????????? ????????? ?????????????????? ??????????????? ????????? ???????????? ?????? ???????????? -1
            List<Acceptance> acceptances =acceptanceRepository.findAllByUser(foundUser);
            for(Acceptance acceptance: acceptances){
                Post post = acceptance.getPost();
                post.decreaseCnt();
                //???????????? ??? ????????? ???????????????????????? ????????????????????? ???????????? ?????? ??? ?????? ??????
                Boolean deadline = false;
                if(post.getCurrentMember()<post.getMaxCapacity()){
                    post.updateDeadline(deadline);
                }
            }


            userApplicationRepository.deleteByUserId(foundUser.getId());
            acceptanceRepository.deleteByUserId(foundUser.getId());
            List<Stack> stacks =stackRepository.findByUserId(foundUser.getId());
            for (Stack stack : stacks){
                stack.setStack(null);
                stack.setUserId(0L);
            }
            foundUser.setUsername(username);
            foundUser.setNickname(nickname);
            foundUser.setPassword(password);
            foundUser.setProfileImg(null);
            foundUser.setImgkey(null);
            foundUser.setRole(UserRoleEnum.USER);
            foundUser.setStacks(null);
            foundUser.setKakaoId(null);
//            userRepository.save(foundUser);
        }
    }

    private List<Stack> tostackByUserId(List<StackDto> requestDto, User user) {
        List<Stack> stackList = new ArrayList<>();
        for(StackDto stackdto : requestDto){
            stackList.add(new Stack(stackdto, user));
        }
        return stackList;
    }
}