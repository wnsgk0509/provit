package com.provit.controller.community;

import com.provit.dto.common.PageResponseDTO;
import com.provit.dto.community.PostDTO;
import com.provit.dto.community.PostSearchDTO;
import com.provit.dto.response.ApiResponse;
import com.provit.service.community.PostService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.provit.common.annotation.LoginUser;
import com.provit.common.ResponseCode;

/**
 * 프론트엔드(React)의 요청을 받는 게시판 컨트롤러 (웨이터 역할)
 */
@RestController // @Controller + @ResponseBody 기능: 모든 반환값을 JSON으로 자동 변환해줍니다.
@RequestMapping("/api/community/posts") // 이 컨트롤러의 기본 URL을 지정합니다.
public class PostController {

    private final PostService postService;
    // 조회수 어뷰징 방지를 위한 인메모리 저장소 (IP_게시글번호 -> 만료시간)
    private final java.util.concurrent.ConcurrentHashMap<String, Long> viewCache = new java.util.concurrent.ConcurrentHashMap<>();

    // Service(주방장)를 주입받습니다.
    @Autowired
    public PostController(PostService postService) {
        this.postService = postService;
    }

    // 클라이언트의 실제 IP를 가져오는 유틸 메서드
    private String getClientIp(javax.servlet.http.HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 게시글 목록을 조회합니다. (페이징, 검색 기능 포함)
     * URL 호출 예시: GET /api/community/posts?page=2&categoryNum=1&keyword=안녕
     */
    @GetMapping
    public ApiResponse<PageResponseDTO<PostDTO>> getPostList(@ModelAttribute PostSearchDTO searchDto) {
        searchDto.setAuthorUserNum(null);
        // 1. 웨이터가 프론트에서 온 파라미터(searchDto)를 그대로 주방장(Service)에게 전달해 요리를 부탁합니다.
        PageResponseDTO<PostDTO> responseData = postService.getPostList(searchDto);

        // 2. 완성된 요리를 규격화된 공통 접시(ApiResponse)에 담아 손님(React)에게 서빙합니다.
        return ApiResponse.success(responseData);
    }

    @GetMapping("/me")
    public ApiResponse<PageResponseDTO<PostDTO>> getMyPostList(
            @ModelAttribute PostSearchDTO searchDto,
            @LoginUser Long userNum) {
        if (userNum == null) return new ApiResponse<>(ResponseCode.AUTH_UNAUTHORIZED, null);
        searchDto.setAuthorUserNum(userNum);
        return ApiResponse.success(postService.getPostList(searchDto));
    }

    /**
     * 인기 게시글 목록을 조회합니다.
     */
    @GetMapping("/popular")
    public ApiResponse<java.util.List<PostDTO>> getPopularPosts(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "5") int limit) {
        java.util.List<PostDTO> popularPosts = postService.getPopularPosts(limit);
        return ApiResponse.success(popularPosts);
    }

    /**
     * 게시글 상세 정보를 조회합니다.
     * URL 호출 예시: GET /api/community/posts/15
     */
    @GetMapping("/{postNum}")
    public ApiResponse<PostDTO> getPostDetail(
            @org.springframework.web.bind.annotation.PathVariable Long postNum,
            @LoginUser Long userNum,
            javax.servlet.http.HttpServletRequest request,
            javax.servlet.http.HttpServletResponse response) {
        
        // 1. IP 기반 1차 검증 (시크릿 모드 대응)
        String clientIp = getClientIp(request);
        String cacheKey = clientIp + "_" + postNum;
        long currentTime = System.currentTimeMillis();
        
        boolean hasViewed = false;
        
        // 메모리에 기록이 남아있고, 아직 만료(1시간)되지 않았다면 이미 조회한 것으로 간주
        if (viewCache.containsKey(cacheKey) && viewCache.get(cacheKey) > currentTime) {
            hasViewed = true;
        }

        // 2. 쿠키 기반 2차 검증 (기존 로직 유지)
        if (!hasViewed) {
            javax.servlet.http.Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (javax.servlet.http.Cookie cookie : cookies) {
                    if (cookie.getName().equals("viewed_post_" + postNum)) {
                        hasViewed = true;
                        break;
                    }
                }
            }
        }

        // 조회하지 않은 경우에만 조회수를 증가시킵니다.
        PostDTO postDetail = postService.getPostDetail(postNum, userNum, !hasViewed);

        // 첫 조회라면 쿠키를 생성하고 IP 캐시에 기록합니다.
        if (!hasViewed) {
            // 메모리 누수 방지 (캐시가 너무 커지면 초기화)
            if (viewCache.size() > 50000) {
                viewCache.clear();
            }
            viewCache.put(cacheKey, currentTime + (60 * 60 * 1000L)); // 1시간 (밀리초)

            javax.servlet.http.Cookie cookie = new javax.servlet.http.Cookie("viewed_post_" + postNum, "true");
            cookie.setMaxAge(60 * 60); // 1시간 (3600초)
            cookie.setPath("/");
            response.addCookie(cookie);
        }

        return ApiResponse.success(postDetail);
    }

    /**
     * 게시글 좋아요(공감)를 토글(On/Off)합니다.
     */
    @org.springframework.web.bind.annotation.PostMapping("/{postNum}/like")
    public ApiResponse<java.util.Map<String, Object>> togglePostLike(
            @org.springframework.web.bind.annotation.PathVariable Long postNum,
            @LoginUser Long userNum) {
        if (userNum == null) return new ApiResponse<>(ResponseCode.AUTH_UNAUTHORIZED, null);
        java.util.Map<String, Object> result = postService.togglePostLike(postNum, userNum);
        return ApiResponse.success(result);
    }

    /**
     * 새 게시글을 등록합니다.
     */
    @org.springframework.web.bind.annotation.PostMapping
    public ApiResponse<Long> createPost(@org.springframework.web.bind.annotation.RequestBody PostDTO postDto, @LoginUser Long userNum) {
        if (userNum == null) return new ApiResponse<>(ResponseCode.AUTH_UNAUTHORIZED, null);
        postDto.setUserNum(userNum);
        Long createdPostNum = postService.createPost(postDto);
        return ApiResponse.success(createdPostNum);
    }

    /**
     * 기존 게시글을 수정합니다.
     */
    @org.springframework.web.bind.annotation.PutMapping("/{postNum}")
    public ApiResponse<Void> updatePost(
            @org.springframework.web.bind.annotation.PathVariable Long postNum, 
            @org.springframework.web.bind.annotation.RequestBody PostDTO postDto,
            @LoginUser Long userNum) {
        if (userNum == null) return new ApiResponse<>(ResponseCode.AUTH_UNAUTHORIZED, null);
        // 안전을 위해 URL의 번호를 DTO에 세팅합니다.
        postDto.setPostNum(postNum);
        postDto.setUserNum(userNum);
        postService.updatePost(postDto);
        return ApiResponse.success();
    }

    /**
     * 특정 게시글을 삭제합니다.
     */
    @org.springframework.web.bind.annotation.DeleteMapping("/{postNum}")
    public ApiResponse<Void> deletePost(
            @org.springframework.web.bind.annotation.PathVariable Long postNum,
            @LoginUser Long userNum) {
        if (userNum == null) return new ApiResponse<>(ResponseCode.AUTH_UNAUTHORIZED, null);
        postService.deletePost(postNum, userNum);
        return ApiResponse.success();
    }
}
