import { useEffect, useState } from 'react';
import { ChevronRight } from 'lucide-react';
import { Link } from 'react-router-dom';
import { fetchMyPosts } from '../../../api/communityApi';
import { fetchMyComments } from '../../../api/commentApi';

const INITIAL_STATE = {
    items: [],
    loading: true,
    error: '',
};

function MyCommunityActivity() {
    const [postsState, setPostsState] = useState(INITIAL_STATE);
    const [commentsState, setCommentsState] = useState(INITIAL_STATE);

    useEffect(() => {
        let isActive = true;

        fetchMyPosts({ page: 1, pageSize: 5 })
            .then((response) => {
                if (!isActive) return;
                setPostsState({
                    items: response?.data?.list || [],
                    loading: false,
                    error: '',
                });
            })
            .catch(() => {
                if (!isActive) return;
                setPostsState({
                    items: [],
                    loading: false,
                    error: '내 게시글을 불러오지 못했습니다.',
                });
            });

        fetchMyComments()
            .then((response) => {
                if (!isActive) return;
                setCommentsState({
                    items: (response?.data || []).slice(0, 5),
                    loading: false,
                    error: '',
                });
            })
            .catch(() => {
                if (!isActive) return;
                setCommentsState({
                    items: [],
                    loading: false,
                    error: '내 댓글을 불러오지 못했습니다.',
                });
            });

        return () => {
            isActive = false;
        };
    }, []);

    return (
        <section className="mypage-community-activity" aria-labelledby="community-activity-title">
            <div className="mypage-community-heading">
                <div>
                    <span>COMMUNITY</span>
                    <h2 id="community-activity-title">나의 커뮤니티 활동</h2>
                    <p>작성한 게시글과 댓글을 최근 활동 순으로 확인하세요.</p>
                </div>
                <Link to="/community" className="mypage-community-all-link">
                    커뮤니티 가기 <ChevronRight size={17} aria-hidden="true" />
                </Link>
            </div>

            <div className="mypage-community-grid">
                <ActivityPanel title="내가 쓴 게시글" state={postsState} emptyMessage="작성한 게시글이 없습니다.">
                    {(post) => (
                        <Link key={post.postNum} className="mypage-community-item" to={`/community/${post.postNum}`}>
                            <span className="mypage-community-category">{post.categoryName || '커뮤니티'}</span>
                            <strong>{post.postTitle || '제목 없음'}</strong>
                            <small>댓글 {post.commentCount || 0} · {formatDate(post.postDate)}</small>
                        </Link>
                    )}
                </ActivityPanel>

                <ActivityPanel title="내가 쓴 댓글" state={commentsState} emptyMessage="작성한 댓글이 없습니다.">
                    {(comment) => (
                        <Link key={comment.commentNum} className="mypage-community-item" to={`/community/${comment.postNum}`}>
                            <strong>{comment.postTitle || '원글 제목 없음'}</strong>
                            <span className="mypage-community-comment">{comment.commentContent || '내용 없음'}</span>
                            <small>{formatDate(comment.commentDate)}</small>
                        </Link>
                    )}
                </ActivityPanel>
            </div>
        </section>
    );
}

function ActivityPanel({ title, state, emptyMessage, children }) {
    return (
        <article className="mypage-community-panel">
            <h3>{title}</h3>
            {state.loading && <div className="mypage-community-state" role="status">불러오는 중입니다.</div>}
            {!state.loading && state.error && <div className="mypage-community-state is-error" role="alert">{state.error}</div>}
            {!state.loading && !state.error && state.items.length === 0 && (
                <div className="mypage-community-state">{emptyMessage}</div>
            )}
            {!state.loading && !state.error && state.items.length > 0 && (
                <div className="mypage-community-list">{state.items.map(children)}</div>
            )}
        </article>
    );
}

function formatDate(value) {
    if (!value) return '-';

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return String(value).slice(0, 10).replaceAll('-', '.');

    return new Intl.DateTimeFormat('ko-KR', {
        month: 'numeric',
        day: 'numeric',
    }).format(date);
}

export default MyCommunityActivity;
