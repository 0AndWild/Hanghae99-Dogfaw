package com.project.dogfaw.post.dto;

import com.project.dogfaw.post.model.Post;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor

public class PostDetailResponseDto {
    private Long id;
    private String title;
    private boolean onLine;
    private String stack;
    private String period;
    private int startAt;
    private int maxCapacity;
    private String content;
    private String nickname;
    private String profileImg;
    private int deadline;
    private boolean bookMarkStatus;

    public PostDetailResponseDto(Post post){
        this.id = post.getId();
        this.title = post.getTitle();
        this.onLine = post.getOnline();
        this.stack = post.getStack();
        this.period = post.getPeriod();
        this.startAt = post.getStartAt();
        this.maxCapacity = post.getMaxCapacity();
        this.content = post.getContent();
        this.nickname = post.getNickname();
        this.profileImg = post.getProfileImg();
        this.deadline = post.getDeadline();
        //this.bookMarkStatus = post.getBookMarkStatus();


    }
}

