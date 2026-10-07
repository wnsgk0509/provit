import React, { useState, useEffect } from 'react';
import { fetchPostList } from '../../api/communityApi';
import { Link, useSearchParams, useNavigate } from 'react-router-dom';
import StudyListSection from '../../components/study/StudyListSection';
import PopularPostsWidget from '../../components/community/PopularPostsWidget';
import PolicyWidget from '../../components/community/PolicyWidget';
import { useAuth } from '../../context/AuthContext';

const CATEGORIES = [
    { id: '', name: '전체' },
    { id: 1, name: '질문' },
    { id: 2, name: '정보' },
    { id: 3, name: '후기' },
    { id: 'study', name: '스터디 모집' },
];

function CommunityList() {
    const { isLoggedIn } = useAuth();
    const navigate = useNavigate();
    const [searchParams, setSearchParams] = useSearchParams();
    const initialCategory = searchParams.get('tab') === 'study' || searchParams.get('category') === 'study' ? 'study' : '';

    const [createStudyCounter, setCreateStudyCounter] = useState(0);

    // 상태 관리
    const [posts, setPosts] = useState([]);
    const [pageInfo, setPageInfo] = useState({});
    const [loading, setLoading] = useState(false);

    // 검색 파라미터 상태
    const [params, setParams] = useState({
        page: 1,
        categoryNum: initialCategory,
        searchType: 'TITLE',
        keyword: ''
    });

    // 폼 입력 상태 (검색 버튼 누르기 전)
    const [searchInput, setSearchInput] = useState({
        searchType: 'TITLE',
        keyword: ''
    });

    // 게시글 목록 가져오기 (카테고리가 'study'가 아닐 때만 호출)
    const loadPosts = async () => {
        if (params.categoryNum === 'study') return;

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

    const handleWriteClick = (e) => {
        if (!isLoggedIn) {
            e.preventDefault();
            alert('로그인이 필요한 서비스입니다. 로그인 페이지로 이동합니다.');
            navigate('/login');
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
            keyword: '', // 탭 전환 시 검색어 초기화
            page: 1 // 카테고리 변경 시 1페이지로 리셋
        });
        
        // 검색창 입력 폼도 초기화
        setSearchInput({
            ...searchInput,
            keyword: ''
        });

        if (categoryId === 'study') {
            setSearchParams({ tab: 'study' });
        } else {
            setSearchParams({});
        }
    };

    // 검색 실행 핸들러
    const handleSearch = (e) => {
        e.preventDefault();
        setParams({
            ...params,
            searchType: searchInput.searchType,
            keyword: searchInput.keyword,
            page: 1, // 검색 시 1페이지로 리셋
            ...(params.categoryNum !== 'study' && { categoryNum: '' }) // 스터디 탭이 아니면 전체 카테고리로 리셋
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
        return dateString.split(' ')[0];
    };

    return (
        <div className="container pt-4 pb-4">
            {/* 상단 타이틀 및 검색 영역 (모바일: 세로 2줄, 데스크톱: 가로 1줄) */}
            <div className="d-flex flex-column flex-md-row justify-content-between align-items-start align-items-md-end gap-4 gap-md-0 mb-2">
                {/* 1. 페이지 타이틀 영역 (좌측/상단) */}
                <div>
                    <div className="text-primary fw-bold" style={{ fontSize: '0.8rem', letterSpacing: '1px' }}>PROVIT COMMUNITY</div>
                    <h3 className="fw-bold mt-1 text-dark mb-0" style={{ fontSize: '1.75rem' }}>취업 커뮤니티</h3>
                </div>

                {/* 2. 검색 영역 (우측/하단) */}
                <div className="w-100" style={{ maxWidth: '350px', minHeight: '32px' }}>
                    <form className="d-flex gap-2 w-100" onSubmit={handleSearch}>
                        <div className="position-relative flex-grow-1">
                            <input
                                type="text"
                                className="form-control px-4 w-100"
                                placeholder={params.categoryNum === 'study' ? "스터디 이름을 검색하세요" : "제목, 내용, 작성자로 검색해보세요"}
                                value={searchInput.keyword}
                                onChange={(e) => setSearchInput({ ...searchInput, keyword: e.target.value })}
                                style={{ borderRadius: '20px', fontSize: '0.85rem', color: 'darkgray', height: '32px' }}
                            />
                        </div>
                        <button 
                            className="btn btn-primary rounded-pill flex-shrink-0 d-flex align-items-center justify-content-center" 
                            type="submit"
                            style={{ height: '32px', fontSize: '0.85rem', padding: '0 1rem' }}
                        >
                            검색
                        </button>
                    </form>
                </div>
            </div>

            <div className="row">
                <div className="col-lg-8">
                    {/* 카테고리 탭 (스크롤 바 제거, 반응형 패딩으로 한 줄에 맞춤) */}
                    <ul className="nav nav-tabs mb-4 border-bottom mt-3 align-items-end">
                        {CATEGORIES.map((cat, index) => (
                            <li className="nav-item" key={cat.id === '' ? 'all' : cat.id}>
                                <button
                                    className={`nav-link px-2 px-md-3 py-2 ${index === 0 ? 'ms-1' : ''} ${params.categoryNum === cat.id ? 'active fw-bold border-bottom-0' : 'text-secondary border-0'}`}
                                    onClick={() => handleCategoryClick(cat.id)}
                                    type="button"
                                    style={{ backgroundColor: 'transparent', whiteSpace: 'nowrap' }}
                                >
                                    {cat.name}
                                </button>
                            </li>
                        ))}
                        {/* 데스크톱 전용 글쓰기 버튼 (모바일에서는 숨김 처리) */}
                        <li className="nav-item ms-auto mb-1 d-none d-md-block flex-shrink-0">
                            {params.categoryNum !== 'study' ? (
                                <Link 
                                    to="/community/write" 
                                    className="btn btn-sm px-4 rounded-pill text-white" 
                                    style={{ backgroundColor: '#5c7c99', border: 'none' }}
                                    onClick={handleWriteClick}
                                >
                                    글쓰기
                                </Link>
                            ) : (
                                <button 
                                    className="btn btn-sm px-4 rounded-pill text-white"
                                    style={{ backgroundColor: '#677381', border: 'none' }}
                                    onClick={() => {
                                        if (isLoggedIn) {
                                            setCreateStudyCounter(prev => prev + 1);
                                        } else {
                                            alert('로그인이 필요한 서비스입니다. 로그인 페이지로 이동합니다.');
                                            navigate('/login');
                                        }
                                    }}
                                >
                                    스터디 만들기
                                </button>
                            )}
                        </li>
                    </ul>

                    {/* 모바일 전용 글쓰기 버튼 (데스크톱에서는 숨김 처리) */}
                    <div className="d-block d-md-none mb-4">
                        {params.categoryNum !== 'study' ? (
                            <Link 
                                to="/community/write" 
                                className="btn w-100 rounded-pill fw-bold py-1 shadow-sm text-center d-block text-white" 
                                style={{ backgroundColor: '#5c7c99', border: 'none', fontSize: '0.9rem' }}
                                onClick={handleWriteClick}
                            >
                                📝 글쓰기
                            </Link>
                        ) : (
                            <button 
                                className="btn w-100 rounded-pill fw-bold py-1 shadow-sm text-white"
                                style={{ backgroundColor: '#677381', border: 'none', fontSize: '0.9rem' }}
                                onClick={() => {
                                    if (isLoggedIn) {
                                        setCreateStudyCounter(prev => prev + 1);
                                    } else {
                                        alert('로그인이 필요한 서비스입니다. 로그인 페이지로 이동합니다.');
                                        navigate('/login');
                                    }
                                }}
                            >
                                스터디 만들기
                            </button>
                        )}
                    </div>

                    {/* 카테고리가 '스터디 모집'인 경우: 스터디 카드 목록 및 개설 영역 렌더링 */}
                    {params.categoryNum === 'study' ? (
                        <StudyListSection createCounter={createStudyCounter} keyword={params.keyword} />
                    ) : (
                        /* 일반 게시판 영역 */
                        <>


                            {/* 게시글 목록 카드 뷰 */}
                            <div className="d-flex flex-column gap-3">
                                {loading ? (
                                    <div className="text-center text-muted py-5">
                                        <div className="spinner-border text-primary" role="status">
                                            <span className="visually-hidden">Loading...</span>
                                        </div>
                                    </div>
                                ) : posts.length > 0 ? (
                                    posts.map((post) => (
                                        <Link
                                            to={`/community/${post.postNum}`}
                                            key={post.postNum}
                                            className="text-decoration-none text-dark"
                                        >
                                            <div className="card border-0 rounded-4 p-3 shadow-sm" style={{ transition: 'all 0.2s ease', cursor: 'pointer' }}
                                                onMouseOver={(e) => e.currentTarget.classList.add('shadow')}
                                                onMouseOut={(e) => e.currentTarget.classList.remove('shadow')}>
                                                <div className="d-flex align-items-center gap-2 mb-2">
                                                    <span className={`badge rounded-pill fw-medium ${post.categoryNum === 1 ? 'bg-primary bg-opacity-10 text-primary' :
                                                        post.categoryNum === 2 ? 'bg-success bg-opacity-10 text-success' :
                                                            post.categoryNum === 3 ? 'bg-info bg-opacity-10 text-info' : 'bg-secondary bg-opacity-10 text-secondary'
                                                        }`}>
                                                        {post.categoryName}
                                                    </span>
                                                    <h6 className="fw-bold mb-0 text-truncate" style={{ maxWidth: '70%' }}>{post.postTitle}</h6>
                                                    {post.commentCount > 0 && (
                                                        <span className="text-danger small fw-bold">[{post.commentCount}]</span>
                                                    )}
                                                </div>
                                                <div className="d-flex align-items-center justify-content-between text-muted small mt-1">
                                                    <div className="d-flex align-items-center gap-2">
                                                        <span>{post.userNickname || '익명'}</span>
                                                        <span style={{ fontSize: '10px' }}>•</span>
                                                        <span>{formatDate(post.postDate)}</span>
                                                    </div>
                                                    <div className="d-flex flex-column align-items-end gap-1" style={{ fontSize: '11px' }}>
                                                        <span>조회수 {post.viewCount || 0}</span>
                                                        <span>좋아요 {post.postLikeCount || 0}</span>
                                                    </div>
                                                </div>
                                            </div>
                                        </Link>
                                    ))
                                ) : (
                                    <div className="text-center text-muted py-5 border rounded-4 bg-light">
                                        등록된 게시글이 없습니다.
                                    </div>
                                )}
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
                        </>
                    )}
                </div>

                {/* 우측 인기글 및 정책 위젯 */}
                <div className="col-lg-4">
                    <PopularPostsWidget />
                    <PolicyWidget />
                </div>
            </div>
        </div>
    );
}

export default CommunityList;
