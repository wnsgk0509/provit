import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { fetchPostDetail, deletePost, togglePostLike } from '../../api/communityApi';
import { useAuth } from '../../context/AuthContext';
import { useModal } from '../../context/ModalContext';
import CommentSection from '../../components/community/CommentSection';
import ReportModal from '../../components/community/ReportModal';
import MDEditor from '@uiw/react-md-editor';
import rehypeSanitize from 'rehype-sanitize';

function CommunityDetail() {
    const { postNum } = useParams();
    const navigate = useNavigate();
    const { user } = useAuth();
    const { showAlert, showConfirm, showToast } = useModal();
    
    const [post, setPost] = useState(null);
    const [loading, setLoading] = useState(true);
    const [showReportModal, setShowReportModal] = useState(false);
    const fetchRef = useRef(null);

    useEffect(() => {
        if (fetchRef.current === postNum) return;
        fetchRef.current = postNum;
        loadPostDetail();
    }, [postNum]);

    const loadPostDetail = async () => {
        setLoading(true);
        try {
            const result = await fetchPostDetail(postNum);
            if (result && result.responseCode && result.responseCode.code === 200) {
                setPost(result.data);
            } else {
                await showAlert(result?.message || '게시글을 불러오는데 실패했습니다.', { type: 'error' });
                navigate('/community');
            }
        } catch (error) {
            await showAlert('서버와의 통신 오류가 발생했습니다.', { type: 'error' });
            navigate('/community');
        } finally {
            setLoading(false);
        }
    };

    const handleDelete = async () => {
        const ok = await showConfirm('정말로 이 게시글을 삭제하시겠습니까?', {
            title: '게시글 삭제',
            type: 'warning',
            confirmText: '삭제',
            cancelText: '취소',
            isDestructive: true
        });
        if (ok) {
            try {
                const result = await deletePost(postNum);
                if (result && result.responseCode && result.responseCode.code === 200) {
                    showToast('게시글이 삭제되었습니다.', 'info');
                    navigate('/community');
                } else {
                    showAlert(result?.message || '삭제에 실패했습니다.', { type: 'error' });
                }
            } catch (error) {
                showAlert('서버 오류로 삭제에 실패했습니다.', { type: 'error' });
            }
        }
    };

    const handleLike = async () => {
        if (!user) {
            showAlert('로그인 후 이용할 수 있습니다.', { type: 'warning' });
            return;
        }
        try {
            const result = await togglePostLike(postNum);
            if (result && result.responseCode && result.responseCode.code === 200) {
                setPost({
                    ...post,
                    isLiked: result.data.isLiked,
                    postLikeCount: result.data.likeCount
                });
            } else {
                showAlert(result?.message || '처리 중 오류가 발생했습니다.', { type: 'error' });
            }
        } catch (error) {
            showAlert('서버와의 통신 중 오류가 발생했습니다.', { type: 'error' });
        }
    };

    const handleReport = () => {
        if (!user) {
            showAlert('로그인 후 이용할 수 있습니다.', { type: 'warning' });
            return;
        }
        setShowReportModal(true);
    };

    const handleShare = async () => {
        const shareData = {
            title: post?.postTitle || '',
            text: 'Provit에서 이 게시글을 확인해보세요!',
            url: window.location.href,
        };

        // 1. 모바일 기기의 기본 공유 창 띄우기 (HTTPS 환경에서만 동작)
        if (navigator.share && window.isSecureContext) {
            try {
                await navigator.share(shareData);
            } catch (err) {
                console.log('공유 취소 또는 실패', err);
            }
        } else {
            // 2. HTTPS가 아닌 로컬 테스트(http://192.168...) 환경이거나 PC인 경우 클립보드 복사로 대체
            try {
                if (navigator.clipboard && window.isSecureContext) {
                    await navigator.clipboard.writeText(window.location.href);
                } else {
                    // HTTP 로컬 테스트 환경을 위한 Fallback (예전 방식)
                    const textArea = document.createElement("textarea");
                    textArea.value = window.location.href;
                    document.body.appendChild(textArea);
                    textArea.select();
                    document.execCommand("copy");
                    document.body.removeChild(textArea);
                }
                showToast('게시글 주소가 복사되었습니다!', 'success');
            } catch (err) {
                console.error(err);
                showAlert('주소 복사에 실패했습니다. 브라우저 주소창에서 직접 복사해주세요.', { type: 'warning' });
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
    const isAdmin = user && user.userType === 'ADMIN';

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
                    <div className="mb-3">
                        <h3 className="card-title fw-bold mb-0 text-break">{post.postTitle}</h3>
                    </div>
                    <div className="d-flex justify-content-between text-muted small">
                        <span><strong>{post.userNickname || '익명'}</strong></span>
                        <span>{formatDateTime(post.postDate)}</span>
                    </div>
                </div>
                
                <div className="card-body p-4" style={{ minHeight: '300px', whiteSpace: 'pre-wrap' }}>
                    {post.postFile && (
                        <div className="mb-4 text-center">
                            <img 
                                src={`/uploads/post_uploadfile/${post.postFile}`} 
                                alt="첨부 이미지" 
                                className="img-fluid rounded shadow-sm" 
                                style={{ maxHeight: '500px' }}
                            />
                        </div>
                    )}
                    <div data-color-mode="light">
                        <MDEditor.Markdown 
                            source={post.postContent} 
                            style={{ whiteSpace: 'pre-wrap', backgroundColor: 'transparent' }} 
                            rehypePlugins={[[rehypeSanitize]]} 
                        />
                    </div>
                </div>
                
                {/* 좋아요 및 공유 버튼, 신고 버튼 */}
                <div className="card-footer bg-white border-top py-3">
                    <div className="d-flex justify-content-between align-items-center">
                        <div style={{ width: '80px' }} className="d-none d-sm-block"></div>
                        
                        <div className="d-flex justify-content-center gap-3">
                            <button 
                                className={`btn btn-sm px-3 py-1 rounded-pill ${post.isLiked ? 'btn-danger' : 'btn-outline-danger'}`}
                                style={{ fontSize: '0.85rem' }}
                                onClick={handleLike}
                            >
                                <i className={`bi ${post.isLiked ? 'bi-heart-fill' : 'bi-heart'} me-1`}></i> 
                                좋아요 {post.postLikeCount || 0}
                            </button>
                            <button 
                                className="btn btn-sm btn-outline-secondary px-3 py-1 rounded-pill"
                                style={{ fontSize: '0.85rem' }}
                                onClick={handleShare}
                            >
                                <i className="bi bi-share-fill me-1"></i> 
                                공유하기
                            </button>
                        </div>
                        
                        <div className="text-end" style={{ minWidth: '80px' }}>
                            {!isAuthor && (
                                <button 
                                    className="btn btn-sm btn-outline-danger px-3 py-1"
                                    style={{ fontSize: '0.85rem' }}
                                    onClick={handleReport}
                                    title="신고하기"
                                >
                                    <i className="bi bi-exclamation-triangle-fill me-1"></i>🚨신고
                                </button>
                            )}
                        </div>
                    </div>
                </div>
            </div>

            <div className="d-flex justify-content-between mt-4 mb-4">
                <Link to="/community" className="btn btn-sm btn-secondary px-3 py-1" style={{ fontSize: '0.85rem' }}>
                    목록으로
                </Link>
                
                {(isAuthor || isAdmin) && (
                    <div className="gap-2 d-flex">
                        {isAuthor && (
                            <Link to={`/community/edit/${postNum}`} className="btn btn-sm btn-outline-primary px-3 py-1" style={{ fontSize: '0.85rem' }}>
                                수정
                            </Link>
                        )}
                        <button className="btn btn-sm btn-outline-danger px-3 py-1" style={{ fontSize: '0.85rem' }} onClick={handleDelete}>
                            삭제
                        </button>
                    </div>
                )}
            </div>

            {/* 댓글 영역 컴포넌트 마운트 */}
            <CommentSection postNum={postNum} />

            <ReportModal 
                show={showReportModal} 
                onClose={() => setShowReportModal(false)} 
                targetType="POST" 
                targetNum={postNum} 
            />
        </div>
    );
}

export default CommunityDetail;
