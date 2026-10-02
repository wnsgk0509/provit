import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { fetchPostDetail, updatePost } from '../../api/communityApi';
import { uploadFile, deleteFile } from '../../api/fileApi';
import { useAuth } from '../../context/AuthContext';
import MDEditor from '@uiw/react-md-editor';
import rehypeSanitize from 'rehype-sanitize';

const CATEGORIES = [
    { id: 1, name: '질문' },
    { id: 2, name: '정보' },
    { id: 3, name: '후기' },
];

function CommunityEdit() {
    const { postNum } = useParams();
    const navigate = useNavigate();
    const { user } = useAuth();
    const [loading, setLoading] = useState(false);
    const [initialLoading, setInitialLoading] = useState(true);
    const [file, setFile] = useState(null); // 추가된 첨부파일 상태
    const fileInputRef = React.useRef(null);
    const [deleteExistingFile, setDeleteExistingFile] = useState(false);
    const [originalPostFile, setOriginalPostFile] = useState('');

    const [formData, setFormData] = useState({
        categoryNum: 1,
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
                e.returnValue = ''; 
            }
        };

        window.addEventListener('beforeunload', handleBeforeUnload);

        return () => {
            window.removeEventListener('beforeunload', handleBeforeUnload);
        };
    }, [isDirty]);

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
                    const currentPostFile = post.postFile || '';
                    setFormData({
                        categoryNum: post.categoryNum,
                        postTitle: post.postTitle,
                        postContent: post.postContent,
                        postFile: currentPostFile
                    });
                    setOriginalPostFile(currentPostFile);
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

    const handleClearNewFile = () => {
        setFile(null);
        if (fileInputRef.current) {
            fileInputRef.current.value = "";
        }
    };

    const handleDeleteExistingFile = () => {
        if (window.confirm("기존 첨부파일을 삭제하시겠습니까? (수정 완료 시 영구 삭제됩니다)")) {
            setIsDirty(true);
            setDeleteExistingFile(true);
            setFormData({ ...formData, postFile: '' });
        }
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
            let finalPostFile = formData.postFile;

            // 1. 새로운 파일이 선택되었다면 업로드
            if (file) {
                const uploadResult = await uploadFile(file, 'post', postNum);
                finalPostFile = uploadResult.savedFileName;
                
                // 새로운 파일 업로드 성공 후, 기존 파일이 있었다면 서버에서 삭제 처리
                if (originalPostFile) {
                    try {
                        await deleteFile('post', originalPostFile);
                    } catch (deleteError) {
                        console.error("기존 첨부파일 삭제 실패:", deleteError);
                    }
                }
            } else if (deleteExistingFile && originalPostFile) {
                // 2. 새로운 파일은 없지만 사용자가 기존 파일을 삭제 요청한 경우
                finalPostFile = ''; // DB 업데이트 시 null 처리하기 위해 빈 문자열
                try {
                    await deleteFile('post', originalPostFile);
                } catch (e) {
                    console.error("기존 첨부파일 삭제 실패:", e);
                }
            }

            const postDto = {
                ...formData,
                postNum: postNum, // 수정할 글 번호 필수 포함
                postFile: finalPostFile, // 업로드된 파일명 (또는 기존 파일명) 덮어쓰기
                userNum: user.userNum
            };

            const result = await updatePost(postDto);
            if (result && result.responseCode && result.responseCode.code === 200) {
                alert('게시글이 성공적으로 수정되었습니다.');
                setIsDirty(false); // 서밋 성공 시 경고 해제
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

                        <div className="mb-3">
                            <label htmlFor="postFile" className="form-label fw-semibold">첨부파일(이미지) 수정</label>
                            {formData.postFile && (
                                <div className="mb-2 d-flex align-items-center gap-2">
                                    <span className="text-primary">현재 첨부된 파일: {formData.postFile}</span>
                                    <button type="button" className="btn btn-sm btn-outline-danger py-0" onClick={handleDeleteExistingFile}>삭제</button>
                                </div>
                            )}
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
                                    <button type="button" className="btn btn-outline-danger btn-sm text-nowrap" onClick={handleClearNewFile}>
                                        첨부 취소
                                    </button>
                                )}
                            </div>
                            {file && (
                                <div className="mt-2 text-success fw-bold">
                                    ✨ 교체 대기 중인 파일: {file.name}
                                </div>
                            )}
                            <div className="form-text text-muted">새로운 파일을 업로드하면 기존 파일은 대체됩니다. (10MB 이하 .jpg, .png 등)</div>
                        </div>

                        <div className="mb-4">
                            <label htmlFor="postContent" className="form-label fw-semibold">내용</label>
                            <div data-color-mode="light">
                                <MDEditor
                                    value={formData.postContent}
                                    onChange={(val) => {
                                        setIsDirty(true);
                                        setFormData({ ...formData, postContent: val || '' });
                                    }}
                                    previewOptions={{
                                        rehypePlugins: [[rehypeSanitize]]
                                    }}
                                    height={400}
                                />
                            </div>
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
