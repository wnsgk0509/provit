import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { fetchPostDetail, deletePost } from '../../api/communityApi';
import { useAuth } from '../../context/AuthContext';

function CommunityDetail() {
    const { postNum } = useParams();
    const navigate = useNavigate();
    const { user } = useAuth();
    
    const [post, setPost] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        loadPostDetail();
    }, [postNum]);

    const loadPostDetail = async () => {
        setLoading(true);
        try {
            const result = await fetchPostDetail(postNum);
            if (result && result.responseCode && result.responseCode.code === 200) {
                setPost(result.data);
            } else {
                alert(result.message || '게시글을 불러오는데 실패했습니다.');
                navigate('/community');
            }
        } catch (error) {
            alert('서버와의 통신 오류가 발생했습니다.');
            navigate('/community');
        } finally {
            setLoading(false);
        }
    };

    const handleDelete = async () => {
        if (window.confirm('정말로 이 게시글을 삭제하시겠습니까?')) {
            try {
                const result = await deletePost(postNum);
                if (result && result.responseCode && result.responseCode.code === 200) {
                    alert('삭제되었습니다.');
                    navigate('/community');
                } else {
                    alert(result.message || '삭제에 실패했습니다.');
                }
            } catch (error) {
                alert('서버 오류로 삭제에 실패했습니다.');
            }
        }
    };

    // 날짜 포맷팅 유틸 (YYYY-MM-DD HH:mm)
    const formatDateTime = (dateString) => {
        if (!dateString) return '';
        // Date가 "2026-09-18 10:00:00" 형태로 온다고 가정
        const parts = dateString.split(':');
        return parts.slice(0, 2).join(':'); // 초 단위는 제외
    };

    if (loading) {
        return (
            <div className="container py-5 text-center">
                <div className="spinner-border text-primary" role="status">
                    <span className="visually-hidden">Loading...</span>
                </div>
            </div>
        );
    }

    if (!post) {
        return null;
    }

    // 작성자인지 확인
    const isAuthor = user && user.userNum === post.userNum;

    return (
        <div className="container py-4" style={{ maxWidth: '900px' }}>
            <div className="card shadow-sm border-0">
                <div className="card-header bg-white border-bottom py-3">
                    <div className="d-flex justify-content-between align-items-center mb-2">
                        <span className={`badge ${
                            post.categoryNum === 1 ? 'bg-primary' :
                            post.categoryNum === 2 ? 'bg-success' :
                            post.categoryNum === 3 ? 'bg-info text-dark' :
                            post.categoryNum === 4 ? 'bg-warning text-dark' : 'bg-secondary'
                        } fs-6`}>
                            {post.categoryName}
                        </span>
                        <span className="text-muted small">
                            조회수 {post.viewCount}
                        </span>
                    </div>
                    <h3 className="card-title fw-bold mb-3">{post.postTitle}</h3>
                    <div className="d-flex justify-content-between text-muted small">
                        <span><strong>{post.userNickname || '익명'}</strong></span>
                        <span>{formatDateTime(post.postDate)}</span>
                    </div>
                </div>
                
                <div className="card-body p-4" style={{ minHeight: '300px', whiteSpace: 'pre-wrap' }}>
                    {post.postContent}
                </div>
                
                {/* 좋아요 버튼 등은 나중에 추가 가능 */}
                <div className="card-footer bg-white border-top text-center py-3">
                    <button className="btn btn-outline-danger px-4 rounded-pill">
                        <i className="bi bi-heart me-1"></i> 좋아요 {post.postLikeCount || 0}
                    </button>
                </div>
            </div>

            <div className="d-flex justify-content-between mt-4">
                <Link to="/community" className="btn btn-secondary">
                    목록으로
                </Link>
                
                {isAuthor && (
                    <div className="gap-2 d-flex">
                        {/* 수정 기능은 추후 구현 */}
                        <button className="btn btn-outline-primary" onClick={() => alert('수정 기능은 준비중입니다.')}>
                            수정
                        </button>
                        <button className="btn btn-outline-danger" onClick={handleDelete}>
                            삭제
                        </button>
                    </div>
                )}
            </div>
        </div>
    );
}

export default CommunityDetail;
