package com.provit.controller.community;

import com.provit.dto.common.PageResponseDTO;
import com.provit.dto.community.PostDTO;
import com.provit.dto.community.PostSearchDTO;
import com.provit.dto.response.ApiResponse;
import com.provit.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 프론트엔드(React)의 요청을 받는 게시판 컨트롤러 (웨이터 역할)
 */
@RestController // @Controller + @ResponseBody 기능: 모든 반환값을 JSON으로 자동 변환해줍니다.
@RequestMapping("/api/community/posts") // 이 컨트롤러의 기본 URL을 지정합니다.
public class PostController {

    private final PostService postService;

    // Service(주방장)를 주입받습니다.
    @Autowired
    public PostController(PostService postService) {
        this.postService = postService;
    }

    /**
     * 게시글 목록을 조회합니다. (페이징, 검색 기능 포함)
     * URL 호출 예시: GET /api/community/posts?page=2&categoryNum=1&keyword=안녕
     */
    @GetMapping
    public ApiResponse<PageResponseDTO<PostDTO>> getPostList(@ModelAttribute PostSearchDTO searchDto) {
        // 1. 웨이터가 프론트에서 온 파라미터(searchDto)를 그대로 주방장(Service)에게 전달해 요리를 부탁합니다.
        PageResponseDTO<PostDTO> responseData = postService.getPostList(searchDto);

        // 2. 완성된 요리를 규격화된 공통 접시(ApiResponse)에 담아 손님(React)에게 서빙합니다.
        return ApiResponse.success(responseData);
    }

    /**
     * 게시글 상세 정보를 조회합니다.
     * URL 호출 예시: GET /api/community/posts/15
     */
    @GetMapping("/{postNum}")
    public ApiResponse<PostDTO> getPostDetail(@org.springframework.web.bind.annotation.PathVariable Long postNum) {
        PostDTO postDetail = postService.getPostDetail(postNum);
        return ApiResponse.success(postDetail);
    }

    /**
     * 새 게시글을 등록합니다.
     */
    @org.springframework.web.bind.annotation.PostMapping
    public ApiResponse<Long> createPost(@org.springframework.web.bind.annotation.RequestBody PostDTO postDto) {
        Long createdPostNum = postService.createPost(postDto);
        return ApiResponse.success(createdPostNum);
    }

    /**
     * 기존 게시글을 수정합니다.
     */
    @org.springframework.web.bind.annotation.PutMapping("/{postNum}")
    public ApiResponse<Void> updatePost(
            @org.springframework.web.bind.annotation.PathVariable Long postNum, 
            @org.springframework.web.bind.annotation.RequestBody PostDTO postDto) {
        // 안전을 위해 URL의 번호를 DTO에 세팅합니다.
        postDto.setPostNum(postNum);
        postService.updatePost(postDto);
        return ApiResponse.success();
    }

    /**
     * 특정 게시글을 삭제합니다.
     */
    @org.springframework.web.bind.annotation.DeleteMapping("/{postNum}")
    public ApiResponse<Void> deletePost(@org.springframework.web.bind.annotation.PathVariable Long postNum) {
        postService.deletePost(postNum);
        return ApiResponse.success();
    }
}
