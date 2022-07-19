package com.project.dogfaw.mypage.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.project.dogfaw.acceptance.Acceptance;
import com.project.dogfaw.acceptance.AcceptanceRepository;
import com.project.dogfaw.apply.model.UserApplication;
import com.project.dogfaw.apply.repository.UserApplicationRepository;
import com.project.dogfaw.bookmark.model.BookMark;
import com.project.dogfaw.bookmark.repository.BookMarkRepository;
import com.project.dogfaw.common.exception.CustomException;
import com.project.dogfaw.common.exception.ErrorCode;
import com.project.dogfaw.common.exception.StatusResponseDto;
import com.project.dogfaw.mypage.dto.AllApplicantsDto;
import com.project.dogfaw.mypage.dto.AllTeammateDto;
import com.project.dogfaw.mypage.dto.MypageRequestDto;
import com.project.dogfaw.mypage.dto.MypageResponseDto;
import com.project.dogfaw.post.dto.PostResponseDto;
import com.project.dogfaw.post.model.Post;
import com.project.dogfaw.post.model.PostStack;
import com.project.dogfaw.post.repository.PostRepository;
import com.project.dogfaw.post.repository.PostStackRepository;
import com.project.dogfaw.user.dto.StackDto;
import com.project.dogfaw.user.model.Stack;
import com.project.dogfaw.user.model.User;
import com.project.dogfaw.user.repository.StackRepository;
import com.project.dogfaw.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class MypageService {

    private final PostRepository postRepository;
    private final BookMarkRepository bookMarkRepository;
    private final PostStackRepository postStackRepository;
    private final UserApplicationRepository userApplicationRepository;
    private final AcceptanceRepository acceptanceRepository;
    private final StackRepository stackRepository;
    private final UserRepository userRepository;

    private final AmazonS3Client amazonS3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    /*내가 북마크한 글 조회*/
    public ArrayList<MypageResponseDto> myBookmark(User user) {

        //유저가 북마크한 것을 리스트로 모두 불러옴
        List<BookMark> userPosts = bookMarkRepository.findAllByUser(user);

        //유저가 북마크한 게시글을 찾아 리스트에 담아주기 위해 ArrayList 생성
        ArrayList<Post> userPostings = new ArrayList<>();

        //BookMarkStatus를 추가적으로 담아줄 ArrayList 생성
        ArrayList<MypageResponseDto> postList = new ArrayList<>();

        //로그인한 유저가 북마크한 게시글들을 ArrayList에 담아줌
        for (BookMark userPost:userPosts){
            Post userPosting = userPost.getPost();
            userPostings.add(userPosting);
        }

        //유저가 북마크한 게시물과 그 게시물의 user 객체를 postList 에 담아서 전달
        for (Post post : userPostings) {
            Long postId = post.getId();
            User writer = post.getUser();

            List<PostStack> postStacks = postStackRepository.findByPostId(postId);
            List<String> stringPostStacks = new ArrayList<>();
            for(PostStack postStack : postStacks){
                stringPostStacks.add(postStack.getStack());
            }

            //PostResponseDto를 이용해 게시글과, 북마크 상태,writer 는 해당 게시글 유저의 프로필 이미지를 불러오기 위함
            MypageResponseDto postDto = new MypageResponseDto(post, stringPostStacks, writer);
            //아까 생성한 ArrayList에 새로운 모양의 값을 담아줌
            postList.add(postDto);
        }
        return postList;
    }

    /*내가 작성한 글 조회*/
    public ArrayList<PostResponseDto> myPost(User user) {

        //유저가 작성한 모든글 리스트로 불러옴///(모든 게시글 X)
        List<Post> posts = postRepository.findByUser(user);
        //유저가 북마크한 것들을 리스트로 불러옴
        List<BookMark> userPosts = bookMarkRepository.findAllByUser(user);

        //유저가 북마크한 게시글을 찾아 리스트에 담아주기 위해 ArrayList 생성
        ArrayList<Post> userPostings = new ArrayList<>();
        //BookMarkStatus를 추가적으로 담아줄 ArrayList 생성
        ArrayList<PostResponseDto> postList = new ArrayList<>();

        //true || false 값을 담아줄 Boolean type의 bookMarkStatus 변수를 하나 생성
        Boolean bookMarkStatus = false ;

        //로그인한 유저가 북마크한 게시글들을 ArrayList에 담아줌
        for (BookMark userPost:userPosts){
            Post userPosting = userPost.getPost();
            userPostings.add(userPosting);
        }

        //일치하면 bookMarkStatus = true 아니면 false를 bookMarkStatus에 담아줌
        for (Post post : posts) {
            Long postId = post.getId();
            User writer = post.getUser();
            for (Post userPost: userPostings ) {
                Long userPostId = userPost.getId();
                //객체를 불러올경우 메모리에 할당되는 주소값으로 불려지기 때문에 비교시 다를 수 밖에 없음
                // 객체 안에있는 특정 데이터 타입으로 비교해줘야 함
                if (postId.equals(userPostId)) {
                    bookMarkStatus = true;
                    break; //true일 경우 탈출
                } else {
                    bookMarkStatus = false;
                }
            }
            List<PostStack> postStacks = postStackRepository.findByPostId(postId);
            List<String> stringPostStacks = new ArrayList<>();
            for(PostStack postStack : postStacks){
                stringPostStacks.add(postStack.getStack());
            }
            PostResponseDto postDto = new PostResponseDto(post, stringPostStacks, bookMarkStatus, writer);
            //아까 생성한 ArrayList에 새로운 모양의 값을 담아줌
            postList.add(postDto);
        }
        return postList;
    }

    /*내가 참여신청한 프로젝트 조회*/
    public ArrayList<PostResponseDto> myApply(User user) {

        //유저가 참여신청한 것을 리스트로 모두 불러옴
        List<UserApplication> userApply = userApplicationRepository.findAllByUser(user);
        //유저가 북마크한 것을 리스트로 모두 불러옴
        List<BookMark> userBookmarks = bookMarkRepository.findAllByUser(user);

        //유저가 참여신청한 게시글을 찾아 리스트에 담아주기 위해 ArrayList 생성
        ArrayList<Post> userApplying = new ArrayList<>();
        //유저가 북마크한 게시글을 찾아 리스트에 담아주기 위해 ArrayList 생성
        ArrayList<Post> userPostings = new ArrayList<>();
        //BookMarkStatus를 추가적으로 담아줄 ArrayList 생성
        ArrayList<PostResponseDto> postList = new ArrayList<>();

        //true || false 값을 담아줄 Boolean type의 bookMarkStatus 변수를 하나 생성
        Boolean bookMarkStatus = false;

        //로그인한 유저가 참여신청한 게시글들을 ArrayList에 담아줌
        for (UserApplication userPost: userApply){
            Post userApplied = userPost.getPost();
            userApplying.add(userApplied);
        }

        //로그인한 유저가 북마크한 게시글을 다 찾아서 ArrayList에 담아줌
        for (BookMark userBookmark: userBookmarks){
            Post userPosting = userBookmark.getPost();
            userPostings.add(userPosting);
        }

        //일치하면 bookMarkStatus = true 아니면 false를 bookMarkStatus에 담아줌
        for (Post post : userApplying ) {
            Long postId = post.getId();
            User writer = post.getUser();
            for (Post userPost: userPostings ) {
                Long userPostId = userPost.getId();
                if (postId.equals(userPostId)) {
                    bookMarkStatus = true;
                    break; //true일 경우 탈출
                } else {
                    bookMarkStatus = false;
                }
            }
            List<PostStack> postStacks = postStackRepository.findByPostId(postId);
            List<String> stringPostStacks = new ArrayList<>();
            for(PostStack postStack : postStacks){
                stringPostStacks.add(postStack.getStack());
            }
            PostResponseDto postDto = new PostResponseDto(post, stringPostStacks, bookMarkStatus, writer);
            //아까 생성한 ArrayList에 새로운 모양의 값을 담아줌
            postList.add(postDto);
        }
        return postList;
    }

    /*참여수락된프로젝트조회*/
    public ArrayList<PostResponseDto> participation(User user) {
        //해당 유저의 참여완료된(수락된) 리스트
        List<Acceptance> acceptances = acceptanceRepository.findAllByUser(user);
        //해당 유저의 북마크 리스트
        List<BookMark> bookMarks = bookMarkRepository.findAllByUser(user);
        //게시물 객체를 담아줄 ArrayList 생성
        ArrayList<Post> acceptedList = new ArrayList<>();
        ArrayList<Post> bookMarkedList = new ArrayList<>();
        //BookMarkStatus를 추가적으로 담아줄 ArrayList 생성
        ArrayList<PostResponseDto> postList = new ArrayList<>();

        Boolean bookMarkStatus = false;

        //해당 유저의 참여완료된 모집글 객체를 하나씩 ArrayList에 담아줌
        for(Acceptance acceptance:acceptances){
            Post post = acceptance.getPost();
            acceptedList.add(post);
        }
        //해당 유저가 북마크한 모집글 객체를 하나씩 ArrayList에 담아줌
        for(BookMark bookMark:bookMarks){
            Post post = bookMark.getPost();
            bookMarkedList.add(post);
        }
        for(Post accepted : acceptedList){
            Long acceptedId = accepted.getId();
            User writer = accepted.getUser();
            for(Post bookMarked:bookMarkedList){
                Long bookMarkedId = bookMarked.getId();

                if (acceptedId.equals(bookMarkedId)){
                    bookMarkStatus = true;
                    break;
                }else {
                    bookMarkStatus = false;
                }
            }
            List<PostStack> postStacks = postStackRepository.findByPostId(acceptedId);
            List<String> stringPostStacks = new ArrayList<>();
            for(PostStack postStack : postStacks){
                stringPostStacks.add(postStack.getStack());
            }
            PostResponseDto postDto = new PostResponseDto(accepted, stringPostStacks, bookMarkStatus, writer);
            postList.add(postDto);
        }
        return postList;
    }

    @Transactional
    /*지원자 전체조회(작성자만)*/
    public ArrayList<AllApplicantsDto> allApplicants(Long postId, User user) {
        //모집글 존재여부 확인
        Post post = postRepository.findById(postId)
                .orElseThrow(()->new CustomException(ErrorCode.POST_NOT_FOUND));
        Long writer = post.getUser().getId();
        //작성자 일치 확인
        if(!writer.equals(user.getId())){
            throw new CustomException(ErrorCode.MYPAGE_INQUIRY_NO_AUTHORITY);
        }
        //해당 게시글의 참여신청을 정보를 다 가져오고 해당 유저 정보를 뽑아와 dto에 담아 리스트로 반환
        List<UserApplication> applicants = userApplicationRepository.findAllByPost(post);
        List<String> stackList = new ArrayList<>();
        ArrayList<AllApplicantsDto> users = new ArrayList<>();

        for(UserApplication applicant:applicants){
            User applier = applicant.getUser();
            List<Stack> stacks = applier.getStacks();
            for (Stack stack: stacks){
                stackList.add(stack.getStack());
            }
            AllApplicantsDto allApplicantsDto =new AllApplicantsDto(applier,stackList);
            users.add(allApplicantsDto);
        }
        return users;
    }

    /*이미지업로드 없이 나머지 유저정보만 편집할때*/
    @Transactional
    public void updateProfile(MypageRequestDto requestDto, User user) {
        //*닉네임 중복검사 후 S3업로드 및 편집
        String nickname = requestDto.getNickname();
        //현재 사용하고 있는 닉네임은 사용가능
        if(!user.getNickname().equals(nickname)){
            if (userRepository.existsByNickname(nickname)) {
                throw new CustomException(ErrorCode.SIGNUP_NICKNAME_OK);
            }
        }
        Long userId = user.getId();
        stackRepository.deleteAllByUserId(userId);
        user.setNickname(requestDto.getNickname());
        List<Stack> stack = stackRepository.saveAll(tostackByUserId(requestDto.getStacks(),user));
        user.updateStack(stack);
    }

    @Transactional
    /*프로필 기본이미지로 변경 요청*/
    public void basicImg(User user) {
        //아마존 S3에 저장된 이미지 삭제
        if(user.getProfileImg()!=null) {
            String imgKey = user.getImgkey();
            amazonS3Client.deleteObject(bucket,imgKey);
        }

        user.setImgkey(null);
        user.setProfileImg(null);
    }

    /*List<String> 형태로 변환*/
    private List<Stack> tostackByUserId(List<StackDto> requestDto, User user) {
        List<Stack> stackList = new ArrayList<>();
        for(StackDto stackdto : requestDto){
            stackList.add(new Stack(stackdto, user));
        }
        return stackList;
    }


    /*내 팀원보기*/
    public ArrayList<AllTeammateDto> checkTeammate(Long postId) {
        //모집글 존재여부 확인
        Post post = postRepository.findById(postId)
                .orElseThrow(()->new CustomException(ErrorCode.POST_NOT_FOUND));

        //해당 게시글 수락 리스트 가져오기
        List<Acceptance> teammates = acceptanceRepository.findAllByPost(post);
        List<String> stackList = new ArrayList<>();
        ArrayList<AllTeammateDto> users = new ArrayList<>();

        for(Acceptance teammate:teammates){
            User teammateUser = teammate.getUser();
            List<Stack> stacks = teammateUser.getStacks();
            for (Stack stack: stacks){
                stackList.add(stack.getStack());
            }
            AllTeammateDto allTeammateDto =new AllTeammateDto(teammateUser,stackList);
            users.add(allTeammateDto);
        }
        return users;
    }

    /*팀원 추방하기*/
    @Transactional
    public ResponseEntity<Object> expulsionTeammate(Long userId, Long postId,User user) {
        //추방하려는 유저정보 찾기
        User teammate = userRepository.findById(userId)
                .orElseThrow(()-> new CustomException(ErrorCode.NOT_FOUND_USER_INFO));
        //해당게식글 찾기
        Post post = postRepository.findById(postId)
                .orElseThrow(()-> new CustomException(ErrorCode.POST_NOT_FOUND));
        //모집글 작성자 확인
        if(!user.getId().equals(postId)){
            throw new CustomException(ErrorCode.MYPAGE_INQUIRY_NO_AUTHORITY);
        }
        //수락정보 존재 확인
        acceptanceRepository.findByUserAndPost(teammate,post)
                .orElseThrow(()-> new CustomException(ErrorCode.ACCEPTANCE_NOT_FOUND));
        //추방하려는 유저,게시글 객체로 찾아 삭제
        acceptanceRepository.deleteByUserAndPost(teammate,post);

        return new ResponseEntity(new StatusResponseDto(teammate.getNickname()+"님 추방이 완료되었습니다",""), HttpStatus.OK);
    }
    @Transactional
    /*참가자 자진 팀 탈퇴*/
    public ResponseEntity<Object> withdrawTeam(Long postId, User user) {
        //해당게식글 찾기
        Post post = postRepository.findById(postId)
                .orElseThrow(()-> new CustomException(ErrorCode.POST_NOT_FOUND));
        //수락정보 존재 확인
        acceptanceRepository.findByUserAndPost(user,post)
                .orElseThrow(()-> new CustomException(ErrorCode.ACCEPTANCE_NOT_FOUND));
        //참여수락db에서 삭제
        acceptanceRepository.deleteByUserAndPost(user,post);

        return new ResponseEntity(new StatusResponseDto("팀 탈퇴가 완료되었습니다",""), HttpStatus.OK);
    }
}
