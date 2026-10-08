package com.provit.dto.study;

public class StudyDTO {
    private Long studyNum;
    private Long userNum;
    private String userNickname; // 개설자 닉네임 (조인 결과)
    private String studyName;
    private String studyExplain;
    private String studyCreateDate;
    private int maxMembers; // 최대 참여 인원

    
    private int memberCount; // 현재 참여 인원 수
    private boolean isJoined; // 현재 로그인한 유저가 참여 중인지 여부

    public Long getStudyNum() {
        return studyNum;
    }

    public void setStudyNum(Long studyNum) {
        this.studyNum = studyNum;
    }

    public Long getUserNum() {
        return userNum;
    }

    public void setUserNum(Long userNum) {
        this.userNum = userNum;
    }

    public String getUserNickname() {
        return userNickname;
    }

    public void setUserNickname(String userNickname) {
        this.userNickname = userNickname;
    }

    public String getStudyName() {
        return studyName;
    }

    public void setStudyName(String studyName) {
        this.studyName = studyName;
    }

    public String getStudyExplain() {
        return studyExplain;
    }

    public void setStudyExplain(String studyExplain) {
        this.studyExplain = studyExplain;
    }

    public String getStudyCreateDate() {
        return studyCreateDate;
    }

    public void setStudyCreateDate(String studyCreateDate) {
        this.studyCreateDate = studyCreateDate;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public boolean getIsJoined() {
        return isJoined;
    }

    public void setIsJoined(boolean isJoined) {
        this.isJoined = isJoined;
    }

    public int getMaxMembers() {
        return maxMembers;
    }

    public void setMaxMembers(int maxMembers) {
        this.maxMembers = maxMembers;
    }
}
