import React, { useState, useEffect } from 'react';
import { fetchPostList } from '../../api/communityApi';
import { Link } from 'react-router-dom';

const CATEGORIES = [
    { id: '', name: '전체' },
    { id: 1, name: '질문' },
    { id: 2, name: '정보' },
    { id: 3, name: '후기' },
    { id: 4, name: '스터디' },
];

function CommunityList() {
    // 상태 관리
    const [posts, setPosts] = useState([]);
    const [pageInfo, setPageInfo] = useState({});
    const [loading, setLoading] = useState(false);
    
    // 검색 파라미터 상태
    const [params, setParams] = useState({
        page: 1,
        categoryNum: '',
        searchType: 'TITLE',
        keyword: ''
    });

    // 폼 입력 상태 (검색 버튼 누르기 전)
    const [searchInput, setSearchInput] = useState({
        searchType: 'TITLE',
        keyword: ''
    });

    // 게시글 목록 가져오기
    const loadPosts = async () => {
        setLoading(true);
        try {
            const result = await fetchPostList(params);
            if (result && result.responseCode && result.responseCode.code === 200) {
                setPosts(result.data.list || []);
                setPageInfo({
                    currentPage: result.data.currentPage,
                    totalPages: result.data.totalPages,
                    startPage: result.data.startPage,
                    endPage: result.data.endPage,
                    hasPrevious: result.data.hasPrevious,
                    hasNext: result.data.hasNext
                });
            } else {
                alert(result.message || '게시글을 불러오는데 실패했습니다.');
            }
        } catch (error) {
            alert('서버와의 통신 오류가 발생했습니다.');
        } finally {
            setLoading(false);
        }
    };

    // params가 변경될 때마다(페이지, 카테고리, 검색 실행 시) API 호출
    useEffect(() => {
        loadPosts();
    }, [params]);

    // 카테고리 탭 클릭 핸들러
    const handleCategoryClick = (categoryId) => {
        setParams({
            ...params,
            categoryNum: categoryId,
            page: 1 // 카테고리 변경 시 1페이지로 리셋
        });
    };

    // 검색 실행 핸들러
    const handleSearch = (e) => {
        e.preventDefault();
        setParams({
            ...params,
            searchType: searchInput.searchType,
            keyword: searchInput.keyword,
            page: 1 // 검색 시 1페이지로 리셋
        });
    };

    // 페이지 클릭 핸들러
    const handlePageChange = (newPage) => {
        if (newPage >= 1 && newPage <= pageInfo.totalPages) {
            setParams({
                ...params,
                page: newPage
            });
        }
    };

    // 날짜 포맷팅 유틸 (YYYY-MM-DD)
    const formatDate = (dateString) => {
        if (!dateString) return '';
        // Date가 "2026-09-18 10:00:00" 형태로 온다고 가정하고 분리
        return dateString.split(' ')[0];
    };

    return (
        <div className="container py-4">
            <h2 className="mb-4 fw-bold">커뮤니티</h2>

            {/* 카테고리 탭 */}
            <ul className="nav nav-tabs mb-4">
                {CATEGORIES.map((cat) => (
                    <li className="nav-item" key={cat.id === '' ? 'all' : cat.id}>
                        <button
                            className={`nav-link ${params.categoryNum === cat.id ? 'active fw-bold' : 'text-secondary'}`}
                            onClick={() => handleCategoryClick(cat.id)}
                            type="button"
                        >
                            {cat.name}
                        </button>
                    </li>
                ))}
            </ul>

            {/* 검색 및 글쓰기 버튼 영역 */}
            <div className="d-flex justify-content-between align-items-center mb-3">
                <form className="d-flex gap-2" onSubmit={handleSearch}>
                    <select
                        className="form-select w-auto"
                        value={searchInput.searchType}
                        onChange={(e) => setSearchInput({ ...searchInput, searchType: e.target.value })}
                    >
                        <option value="TITLE">제목</option>
                        <option value="CONTENT">내용</option>
                        <option value="WRITER">작성자</option>
                    </select>
                    <input
                        type="text"
                        className="form-control"
                        placeholder="검색어를 입력하세요"
                        value={searchInput.keyword}
                        onChange={(e) => setSearchInput({ ...searchInput, keyword: e.target.value })}
                        style={{ width: '250px' }}
                    />
                    <button className="btn btn-outline-primary" type="submit">검색</button>
                </form>
                
                {/* 글쓰기 버튼 */}
                <Link to="/community/write" className="btn btn-primary">
                    글쓰기
                </Link>
            </div>

            {/* 게시글 목록 테이블 */}
            <div className="table-responsive">
                <table className="table table-hover align-middle text-center border-top">
                    <thead className="table-light">
                        <tr>
                            <th scope="col" style={{ width: '8%' }}>번호</th>
                            <th scope="col" style={{ width: '12%' }}>분류</th>
                            <th scope="col" style={{ width: '40%' }}>제목</th>
                            <th scope="col" style={{ width: '15%' }}>작성자</th>
                            <th scope="col" style={{ width: '10%' }}>작성일</th>
                            <th scope="col" style={{ width: '7%' }}>조회</th>
                            <th scope="col" style={{ width: '8%' }}>추천</th>
                        </tr>
                    </thead>
                    <tbody>
                        {loading ? (
                            <tr>
                                <td colSpan="7" className="py-5 text-center text-muted">
                                    <div className="spinner-border text-primary" role="status">
                                        <span className="visually-hidden">Loading...</span>
                                    </div>
                                </td>
                            </tr>
                        ) : posts.length > 0 ? (
                            posts.map((post) => (
                                <tr key={post.postNum}>
                                    <td>{post.postNum}</td>
                                    <td>
                                        <span className={`badge ${
                                            post.categoryNum === 1 ? 'bg-primary' :
                                            post.categoryNum === 2 ? 'bg-success' :
                                            post.categoryNum === 3 ? 'bg-info text-dark' :
                                            post.categoryNum === 4 ? 'bg-warning text-dark' : 'bg-secondary'
                                        }`}>
                                            {post.categoryName}
                                        </span>
                                    </td>
                                    <td className="text-start">
                                        <Link to={`/community/${post.postNum}`} className="text-decoration-none text-dark fw-semibold">
                                            {post.postTitle}
                                        </Link>
                                    </td>
                                    <td>{post.userNickname || '익명'}</td>
                                    <td className="text-muted small">{formatDate(post.postDate)}</td>
                                    <td>{post.viewCount || 0}</td>
                                    <td>{post.postLikeCount || 0}</td>
                                </tr>
                            ))
                        ) : (
                            <tr>
                                <td colSpan="7" className="py-5 text-center text-muted">
                                    게시글이 존재하지 않습니다.
                                </td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </div>

            {/* 페이징 컴포넌트 */}
            {pageInfo.totalPages > 0 && (
                <nav aria-label="Page navigation" className="mt-4">
                    <ul className="pagination justify-content-center">
                        <li className={`page-item ${!pageInfo.hasPrevious ? 'disabled' : ''}`}>
                            <button
                                className="page-link"
                                onClick={() => handlePageChange(pageInfo.startPage - 1)}
                                disabled={!pageInfo.hasPrevious}
                            >
                                이전
                            </button>
                        </li>
                        
                        {/* startPage부터 endPage까지 배열 생성하여 매핑 */}
                        {Array.from({ length: pageInfo.endPage - pageInfo.startPage + 1 }, (_, i) => pageInfo.startPage + i).map(num => (
                            <li key={num} className={`page-item ${pageInfo.currentPage === num ? 'active' : ''}`}>
                                <button className="page-link" onClick={() => handlePageChange(num)}>
                                    {num}
                                </button>
                            </li>
                        ))}

                        <li className={`page-item ${!pageInfo.hasNext ? 'disabled' : ''}`}>
                            <button
                                className="page-link"
                                onClick={() => handlePageChange(pageInfo.endPage + 1)}
                                disabled={!pageInfo.hasNext}
                            >
                                다음
                            </button>
                        </li>
                    </ul>
                </nav>
            )}
        </div>
    );
}

export default CommunityList;
