package com.project.dogfaw.post.dto;

import com.project.dogfaw.post.model.Post;
import com.project.dogfaw.user.model.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor

public class PostDetailResponseDto {
    private Long id;
    private String title;
    private String onLine;
    private List<String> stacks;
    private String period;
    private String startAt;
    private int maxCapacity;
    private String content;
    private String nickname;
    private String profileImg;
    private Boolean deadline;
    private boolean bookMarkStatus;
    private int currentMember;

    private Boolean applyStatus;

    public PostDetailResponseDto(Post post, List<String> stacks, User user, boolean bookMarkStatus, boolean applyStatus){
        this.id = post.getId();
        this.title = post.getTitle();
        this.onLine = post.getOnline();
        this.stacks = stacks;
        this.period = post.getPeriod();
        this.startAt = post.getStartAt();
        this.maxCapacity = post.getMaxCapacity();
        this.content = post.getContent();
        this.nickname = post.getUser().getNickname();
        this.profileImg = user.getProfileImg();
        this.deadline = post.getDeadline();
        this.bookMarkStatus = bookMarkStatus;
        this.currentMember = post.getCurrentMember();
        this.applyStatus = applyStatus;
    }
}

