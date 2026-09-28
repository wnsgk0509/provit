import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { createPost, updatePost, deletePost } from '../../api/communityApi';
import { uploadFile } from '../../api/fileApi';
import { useAuth } from '../../context/AuthContext';

const CATEGORIES = [
    { id: 1, name: '질문' },
    { id: 2, name: '정보' },
    { id: 3, name: '후기' },
];

function CommunityWrite() {
    const navigate = useNavigate();
    const { user } = useAuth();
    const [loading, setLoading] = useState(false);
    const [file, setFile] = useState(null); // 첨부파일 상태 추가
    const fileInputRef = useRef(null);

    const [formData, setFormData] = useState({
        categoryNum: 1, // 기본값: 질문
        postTitle: '',
        postContent: '',
        postFile: ''
    });
    const [isDirty, setIsDirty] = useState(false); // 폼 수정 여부 추적

    // 브라우저 뒤로가기, 새로고침, 탭 닫기 감지 (Native Event)
    useEffect(() => {
        const handleBeforeUnload = (e) => {
            if (isDirty) {
                e.preventDefault();
                e.returnValue = ''; // Chrome 등 모던 브라우저 규격
            }
        };

        window.addEventListener('beforeunload', handleBeforeUnload);

        return () => {
            window.removeEventListener('beforeunload', handleBeforeUnload);
        };
    }, [isDirty]);

    const handleChange = (e) => {
        setIsDirty(true);
        const { name, value } = e.target;
        setFormData({
            ...formData,
            [name]: name === 'categoryNum' ? parseInt(value) : value
        });
    };

    const handleFileChange = (e) => {
        setIsDirty(true);
        if (e.target.files && e.target.files[0]) {
            setFile(e.target.files[0]);
        } else {
            setFile(null);
        }
    };

    const handleClearFile = () => {
        setFile(null);
        if (fileInputRef.current) {
            fileInputRef.current.value = "";
        }
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
                const createdPostNum = result.data; // 서버가 반환한 생성된 글 번호
                
                // 3-Step: 첨부파일이 있는 경우 추가 업로드 진행
                if (file) {
                    try {
                        const uploadResult = await uploadFile(file, 'post', createdPostNum);
                        // 파일 업로드 성공 후, 글 정보 업데이트 (postFile 컬럼 저장)
                        await updatePost({
                            ...postDto,
                            postNum: createdPostNum,
                            postFile: uploadResult.savedFileName 
                        });
                    } catch (uploadError) {
                        console.error("파일 업로드 실패:", uploadError);
                        // 파일 업로드 실패 시 데이터 불일치 방지를 위해 생성된 게시글 롤백(삭제)
                        try {
                            await deletePost(createdPostNum);
                        } catch (rollbackError) {
                            console.error("롤백 처리 중 오류:", rollbackError);
                        }
                        alert("첨부파일 업로드 중 오류가 발생하여 게시글 등록이 취소되었습니다.");
                        return; // 함수 강제 종료 (성공 알럿 띄우지 않음)
                    }
                }

                alert('게시글이 성공적으로 등록되었습니다.');
                setIsDirty(false); // 서밋 성공 시 경고 해제
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

                        <div className="mb-3">
                            <label htmlFor="postFile" className="form-label fw-semibold">첨부파일(이미지)</label>
                            <div className="d-flex gap-2 align-items-center">
                                <input
                                    type="file"
                                    className="form-control"
                                    id="postFile"
                                    name="postFile"
                                    accept="image/*"
                                    onChange={handleFileChange}
                                    ref={fileInputRef}
                                />
                                {file && (
                                    <button type="button" className="btn btn-outline-danger btn-sm text-nowrap" onClick={handleClearFile}>
                                        첨부 취소
                                    </button>
                                )}
                            </div>
                            <div className="form-text text-muted">10MB 이하의 이미지 파일(.jpg, .png 등)만 업로드 가능합니다.</div>
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
