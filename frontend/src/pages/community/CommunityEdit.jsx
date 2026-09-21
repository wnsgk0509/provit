import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { fetchPostDetail, updatePost } from '../../api/communityApi';
import { useAuth } from '../../context/AuthContext';

const CATEGORIES = [
    { id: 1, name: '질문' },
    { id: 2, name: '정보' },
    { id: 3, name: '후기' },
    { id: 4, name: '스터디' },
];

function CommunityEdit() {
    const { postNum } = useParams();
    const navigate = useNavigate();
    const { user } = useAuth();
    const [loading, setLoading] = useState(false);
    const [initialLoading, setInitialLoading] = useState(true);

    const [formData, setFormData] = useState({
        categoryNum: 1,
        postTitle: '',
        postContent: '',
        postFile: ''
    });

    useEffect(() => {
        const loadPost = async () => {
            try {
                const result = await fetchPostDetail(postNum);
                if (result && result.responseCode && result.responseCode.code === 200) {
                    const post = result.data;
                    // 작성자 본인인지 2차 검증 (프론트 단)
                    if (!user || user.userNum !== post.userNum) {
                        alert('수정 권한이 없습니다.');
                        navigate('/community');
                        return;
                    }
                    setFormData({
                        categoryNum: post.categoryNum,
                        postTitle: post.postTitle,
                        postContent: post.postContent,
                        postFile: post.postFile || ''
                    });
                } else {
                    alert('게시글 정보를 불러오지 못했습니다.');
                    navigate('/community');
                }
            } catch (error) {
                alert('서버 오류가 발생했습니다.');
                navigate('/community');
            } finally {
                setInitialLoading(false);
            }
        };
        
        if (user) {
            loadPost();
        } else {
            alert('로그인이 필요합니다.');
            navigate('/login');
        }
    }, [postNum, user, navigate]);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData({
            ...formData,
            [name]: name === 'categoryNum' ? parseInt(value) : value
        });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        
        if (!formData.postTitle.trim()) {
            alert('제목을 입력해주세요.');
            return;
        }

        if (!formData.postContent.trim()) {
            alert('내용을 입력해주세요.');
            return;
        }

        setLoading(true);
        try {
            const postDto = {
                ...formData,
                postNum: postNum, // 수정할 글 번호 필수 포함
                userNum: user.userNum
            };

            const result = await updatePost(postDto);
            if (result && result.responseCode && result.responseCode.code === 200) {
                alert('게시글이 성공적으로 수정되었습니다.');
                navigate(`/community/${postNum}`);
            } else {
                alert(result.message || '수정에 실패했습니다.');
            }
        } catch (error) {
            alert('서버와의 통신 오류가 발생했습니다.');
        } finally {
            setLoading(false);
        }
    };

    const handleCancel = () => {
        if (window.confirm('수정을 취소하시겠습니까? 변경 사항은 저장되지 않습니다.')) {
            navigate(`/community/${postNum}`);
        }
    };

    if (initialLoading) {
        return (
            <div className="container py-5 text-center">
                <div className="spinner-border text-primary" role="status">
                    <span className="visually-hidden">Loading...</span>
                </div>
            </div>
        );
    }

    return (
        <div className="container py-4" style={{ maxWidth: '800px' }}>
            <h2 className="mb-4 fw-bold">게시글 수정</h2>
            <div className="card shadow-sm">
                <div className="card-body p-4">
                    <form onSubmit={handleSubmit}>
                        <div className="mb-3">
                            <label htmlFor="categoryNum" className="form-label fw-semibold">분류</label>
                            <select
                                className="form-select"
                                id="categoryNum"
                                name="categoryNum"
                                value={formData.categoryNum}
                                onChange={handleChange}
                                required
                            >
                                {CATEGORIES.map(cat => (
                                    <option key={cat.id} value={cat.id}>{cat.name}</option>
                                ))}
                            </select>
                        </div>
                        
                        <div className="mb-3">
                            <label htmlFor="postTitle" className="form-label fw-semibold">제목</label>
                            <input
                                type="text"
                                className="form-control"
                                id="postTitle"
                                name="postTitle"
                                placeholder="제목을 입력해주세요 (최대 100자)"
                                value={formData.postTitle}
                                onChange={handleChange}
                                maxLength={100}
                                required
                            />
                        </div>

                        <div className="mb-4">
                            <label htmlFor="postContent" className="form-label fw-semibold">내용</label>
                            <textarea
                                className="form-control"
                                id="postContent"
                                name="postContent"
                                rows="15"
                                placeholder="내용을 입력해주세요"
                                value={formData.postContent}
                                onChange={handleChange}
                                style={{ resize: 'vertical' }}
                                required
                            ></textarea>
                        </div>

                        <div className="d-flex justify-content-end gap-2 mt-4 pt-3 border-top">
                            <button type="button" className="btn btn-secondary px-4" onClick={handleCancel}>
                                취소
                            </button>
                            <button type="submit" className="btn btn-primary px-4" disabled={loading}>
                                {loading ? '수정 중...' : '수정 완료'}
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
}

export default CommunityEdit;
