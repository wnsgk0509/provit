import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createPost } from '../../api/communityApi';
import { useAuth } from '../../context/AuthContext';

const CATEGORIES = [
    { id: 1, name: '질문' },
    { id: 2, name: '정보' },
    { id: 3, name: '후기' },
    { id: 4, name: '스터디' },
];

function CommunityWrite() {
    const navigate = useNavigate();
    const { user } = useAuth();
    const [loading, setLoading] = useState(false);

    const [formData, setFormData] = useState({
        categoryNum: 1, // 기본값: 질문
        postTitle: '',
        postContent: '',
        postFile: ''
    });

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData({
            ...formData,
            [name]: name === 'categoryNum' ? parseInt(value) : value
        });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        
        if (!user) {
            alert('로그인이 필요한 기능입니다.');
            navigate('/login');
            return;
        }

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
            // 현재 로그인된 유저 번호 매핑
            const postDto = {
                ...formData,
                userNum: user.userNum
            };

            const result = await createPost(postDto);
            if (result && result.responseCode && result.responseCode.code === 200) {
                alert('게시글이 성공적으로 등록되었습니다.');
                navigate('/community');
            } else {
                alert(result.message || '게시글 등록에 실패했습니다.');
            }
        } catch (error) {
            alert('서버와의 통신 오류가 발생했습니다.');
        } finally {
            setLoading(false);
        }
    };

    const handleCancel = () => {
        if (window.confirm('작성을 취소하시겠습니까? 작성 중인 내용은 저장되지 않습니다.')) {
            navigate('/community');
        }
    };

    return (
        <div className="container py-4" style={{ maxWidth: '800px' }}>
            <h2 className="mb-4 fw-bold">게시글 작성</h2>
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
                                {loading ? '등록 중...' : '등록'}
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
}

export default CommunityWrite;
