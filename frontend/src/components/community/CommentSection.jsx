import React, { useState, useEffect } from 'react';
import { fetchComments, createComment, updateComment, deleteComment } from '../../api/commentApi';
import { useAuth } from '../../context/AuthContext';

function CommentSection({ postNum }) {
    const { user } = useAuth();
    const [comments, setComments] = useState([]);
    const [newComment, setNewComment] = useState('');
    const [editingId, setEditingId] = useState(null);
    const [editContent, setEditContent] = useState('');

    useEffect(() => {
        loadComments();
    }, [postNum]);

    const loadComments = async () => {
        try {
            const result = await fetchComments(postNum);
            if (result && result.responseCode && result.responseCode.code === 200) {
                setComments(result.data || []);
            }
        } catch (error) {
            console.error('댓글 목록을 불러오는 중 오류 발생');
        }
    };

    const handleCreateSubmit = async (e) => {
        e.preventDefault();
        if (!user) {
            alert('로그인이 필요합니다.');
            return;
        }
        if (!newComment.trim()) {
            alert('내용을 입력해주세요.');
            return;
        }

        try {
            const dto = { userNum: user.userNum, commentContent: newComment };
            const result = await createComment(postNum, dto);
            if (result && result.responseCode && result.responseCode.code === 200) {
                setNewComment('');
                loadComments();
            } else {
                alert('댓글 등록에 실패했습니다.');
            }
        } catch (error) {
            alert('서버 오류가 발생했습니다.');
        }
    };

    const handleEditStart = (comment) => {
        setEditingId(comment.commentNum);
        setEditContent(comment.commentContent);
    };

    const handleEditSubmit = async (commentNum) => {
        if (!editContent.trim()) {
            alert('수정할 내용을 입력해주세요.');
            return;
        }
        try {
            const dto = { userNum: user.userNum, commentContent: editContent };
            const result = await updateComment(postNum, commentNum, dto);
            if (result && result.responseCode && result.responseCode.code === 200) {
                setEditingId(null);
                setEditContent('');
                loadComments();
            } else {
                alert('댓글 수정에 실패했습니다.');
            }
        } catch (error) {
            alert('서버 오류가 발생했습니다.');
        }
    };

    const handleDelete = async (commentNum) => {
        if (!window.confirm('정말 삭제하시겠습니까?')) return;
        try {
            const result = await deleteComment(postNum, commentNum);
            if (result && result.responseCode && result.responseCode.code === 200) {
                loadComments();
            } else {
                alert('삭제에 실패했습니다.');
            }
        } catch (error) {
            alert('서버 오류가 발생했습니다.');
        }
    };

    return (
        <div className="mt-5">
            <h5 className="mb-4">댓글 <span className="text-primary fw-bold">{comments.length}</span></h5>

            {/* 댓글 작성 폼 */}
            <form onSubmit={handleCreateSubmit} className="mb-5">
                <div className="input-group">
                    <textarea 
                        className="form-control" 
                        rows="2" 
                        placeholder={user ? "댓글을 남겨보세요." : "로그인 후 댓글을 남길 수 있습니다."}
                        value={newComment}
                        onChange={(e) => setNewComment(e.target.value)}
                        disabled={!user}
                        style={{ resize: 'none' }}
                    ></textarea>
                    <button className="btn btn-primary px-4" type="submit" disabled={!user}>등록</button>
                </div>
            </form>

            {/* 댓글 목록 */}
            <div className="list-group list-group-flush border-top">
                {comments.length === 0 ? (
                    <div className="text-center py-5 text-muted">첫 번째 댓글을 남겨보세요!</div>
                ) : (
                    comments.map(comment => (
                        <div key={comment.commentNum} className="list-group-item py-3 px-0 border-bottom">
                            <div className="d-flex justify-content-between align-items-center mb-2">
                                <strong>{comment.userNickname}</strong>
                                <small className="text-muted">{comment.commentDate}</small>
                            </div>
                            
                            {editingId === comment.commentNum ? (
                                <div className="mt-2">
                                    <textarea 
                                        className="form-control mb-2" 
                                        rows="2"
                                        value={editContent}
                                        onChange={(e) => setEditContent(e.target.value)}
                                    ></textarea>
                                    <div className="d-flex justify-content-end gap-2">
                                        <button className="btn btn-sm btn-secondary" onClick={() => setEditingId(null)}>취소</button>
                                        <button className="btn btn-sm btn-primary" onClick={() => handleEditSubmit(comment.commentNum)}>수정 완료</button>
                                    </div>
                                </div>
                            ) : (
                                <div>
                                    <p className="mb-2" style={{ whiteSpace: 'pre-wrap' }}>{comment.commentContent}</p>
                                    {user && user.userNum === comment.userNum && (
                                        <div className="d-flex justify-content-end gap-2">
                                            <button className="btn btn-sm btn-link text-muted text-decoration-none p-0" onClick={() => handleEditStart(comment)}>수정</button>
                                            <button className="btn btn-sm btn-link text-danger text-decoration-none p-0" onClick={() => handleDelete(comment.commentNum)}>삭제</button>
                                        </div>
                                    )}
                                </div>
                            )}
                        </div>
                    ))
                )}
            </div>
        </div>
    );
}

export default CommentSection;
