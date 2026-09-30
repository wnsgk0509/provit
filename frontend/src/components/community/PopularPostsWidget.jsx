import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchPopularPosts } from '../../api/communityApi';

const PopularPostsWidget = () => {
    const [popularPosts, setPopularPosts] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const loadPopularPosts = async () => {
            try {
                const data = await fetchPopularPosts(5); // 상위 5개
                if (data.responseCode && data.responseCode.code === 200) {
                    setPopularPosts(data.data || []);
                }
            } catch (error) {
                console.error("인기 게시글 조회 중 오류:", error);
            } finally {
                setLoading(false);
            }
        };

        loadPopularPosts();
    }, []);

    const formatCount = (count) => {
        if (count >= 1000) {
            return (count / 1000).toFixed(1) + 'k';
        }
        return count;
    };

    return (
        <div className="card shadow-sm border-0 mb-4 rounded-4 p-4">
            <div className="d-flex justify-content-between align-items-center mb-3">
                <h5 className="fw-bold mb-0">지금 인기 있는 글</h5>
                <span className="text-muted small">24시간</span>
            </div>
            
            <div className="d-flex flex-column gap-3">
                {loading ? (
                    <div className="text-center text-muted small py-3">로딩 중...</div>
                ) : popularPosts.length > 0 ? (
                    popularPosts.map((post, index) => (
                        <div key={post.postNum} className="d-flex align-items-center justify-content-between">
                            <div className="d-flex align-items-center gap-3 overflow-hidden">
                                <span className="fw-bold text-primary" style={{ minWidth: '20px' }}>
                                    {String(index + 1).padStart(2, '0')}
                                </span>
                                <Link to={`/community/${post.postNum}`} className="text-decoration-none text-dark text-truncate">
                                    {post.postTitle}
                                </Link>
                            </div>
                            <span className="text-muted small ms-2">{formatCount(post.viewCount)}</span>
                        </div>
                    ))
                ) : (
                    <div className="text-muted small">인기 게시글이 없습니다.</div>
                )}
            </div>
        </div>
    );
};

export default PopularPostsWidget;
