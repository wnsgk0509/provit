import { useState } from 'react';
import { Pencil, Trash2 } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { deleteCoverLetter } from '../../../api/documentApi';
import { DocumentDate } from './ResumeRead';

const coverLetterFields = [
    { name: 'growthProcess', label: '성장 과정' },
    { name: 'personalityStrengthsWeaknesses', label: '성격의 장단점' },
    { name: 'problemSolvingExperience', label: '문제 해결 경험' },
    { name: 'postJoiningAspiration', label: '입사 후 포부' },
];

function CoverLetterRead({ document }) {
    const navigate = useNavigate();
    const [isDeleting, setIsDeleting] = useState(false);
    const [deleteError, setDeleteError] = useState('');

    const handleDelete = async () => {
        if (!window.confirm('자기소개서를 삭제하시겠습니까? 삭제한 문서는 복구할 수 없습니다.')) return;

        setIsDeleting(true);
        setDeleteError('');
        try {
            await deleteCoverLetter(document.letterNum);
            navigate('/mypage', { replace: true });
            window.requestAnimationFrame(() => window.scrollTo({ top: 0, behavior: 'auto' }));
        } catch (error) {
            const responseData = error.response?.data;
            setDeleteError(
                responseData?.data
                || responseData?.responseCode?.message
                || '자기소개서를 삭제하지 못했습니다.',
            );
        } finally {
            setIsDeleting(false);
        }
    };

    return (
        <article className="document-form document-read-card">
            <div className="document-form-heading document-read-heading">
                <div>
                    <span>COVER LETTER</span>
                    <h2>{document.coverLetterTitle || '-'}</h2>
                    <p>자기소개서에 작성한 항목별 내용입니다.</p>
                </div>
                <DocumentDate createdAt={document.createdAt} updatedAt={document.updatedAt} />
            </div>

            <section className="document-form-section" aria-labelledby="cover-letter-read-title">
                <div className="document-section-heading">
                    <h3 id="cover-letter-read-title">자기소개서 내용</h3>
                </div>
                <dl className="document-read-letter-list">
                    {coverLetterFields.map((field) => (
                        <div className="document-read-letter-item" key={field.name}>
                            <dt>{field.label}</dt>
                            <dd>{document[field.name] || '작성된 내용이 없습니다.'}</dd>
                        </div>
                    ))}
                </dl>
            </section>

            <div className="document-form-actions">
                <button type="button" className="document-danger-button" onClick={handleDelete} disabled={isDeleting}>
                    <Trash2 size={17} /> {isDeleting ? '삭제 중...' : '자기소개서 삭제'}
                </button>
                <Link
                    className="document-primary-button document-read-edit-link"
                    to={`/documents/cover-letter/${document.letterNum}/edit`}
                >
                    <Pencil size={17} /> 자기소개서 수정
                </Link>
            </div>
            {deleteError && <p className="document-delete-error" role="alert">{deleteError}</p>}
        </article>
    );
}

export default CoverLetterRead;
