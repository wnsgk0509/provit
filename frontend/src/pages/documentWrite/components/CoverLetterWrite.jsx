import { useState } from 'react';
import { createCoverLetter, updateCoverLetter } from '../../../api/documentApi';

const coverLetterFields = [
    { name: 'growthProcess', label: '성장 과정', placeholder: '성장 과정에서 형성된 가치관과 직무에 영향을 준 경험을 작성해 주세요.' },
    { name: 'personalityStrengthsWeaknesses', label: '성격의 장단점', placeholder: '업무에서 드러나는 강점과 보완하고 있는 약점을 작성해 주세요.' },
    { name: 'problemSolvingExperience', label: '문제 해결 경험', placeholder: '문제를 발견하고 해결한 과정과 결과를 구체적으로 작성해 주세요.' },
    { name: 'postJoiningAspiration', label: '입사 후 포부', placeholder: '입사 후 이루고 싶은 목표와 성장 계획을 작성해 주세요.' },
];

const emptyCoverLetter = {
    coverLetterTitle: '',
    growthProcess: '',
    personalityStrengthsWeaknesses: '',
    problemSolvingExperience: '',
    postJoiningAspiration: '',
};

function CoverLetterWrite({ initialData = null, onSaved, onCancel }) {
    const isEditMode = Boolean(initialData?.letterNum);
    const [coverLetter, setCoverLetter] = useState(() => (
        Object.keys(emptyCoverLetter).reduce((normalized, key) => ({
            ...normalized,
            [key]: initialData?.[key] ?? '',
        }), {})
    ));
    const [isSaving, setIsSaving] = useState(false);
    const [saveMessage, setSaveMessage] = useState({ type: '', text: '' });

    const handleChange = (event) => {
        const { name, value } = event.target;
        setCoverLetter((current) => ({ ...current, [name]: value }));
    };

    const handleSubmit = async (event) => {
        event.preventDefault();
        setIsSaving(true);
        setSaveMessage({ type: '', text: '' });

        try {
            const savedCoverLetter = isEditMode
                ? await updateCoverLetter(initialData.letterNum, coverLetter)
                : await createCoverLetter(coverLetter);
            const letterNum = savedCoverLetter?.letterNum;
            if (isEditMode) {
                onSaved?.(savedCoverLetter);
                return;
            }
            setCoverLetter({ ...emptyCoverLetter });
            setSaveMessage({
                type: 'success',
                text: letterNum
                    ? `자기소개서가 저장되었습니다. (자기소개서 번호: ${letterNum})`
                    : '자기소개서가 저장되었습니다.',
            });
        } catch (error) {
            const responseData = error.response?.data;
            setSaveMessage({
                type: 'error',
                text: responseData?.data
                    || responseData?.responseCode?.message
                    || `자기소개서를 ${isEditMode ? '수정' : '저장'}하지 못했습니다. 잠시 후 다시 시도해 주세요.`,
            });
        } finally {
            setIsSaving(false);
        }
    };

    return (
        <form className="document-form" onSubmit={handleSubmit}>
            <div className="document-form-heading">
                <span>COVER LETTER</span>
                <h2>자기소개서 {isEditMode ? '수정' : '작성'}</h2>
                <p>나의 경험과 역량이 드러나도록 항목별 내용을 {isEditMode ? '수정' : '작성'}하세요.</p>
            </div>

            <section className="document-form-section" aria-labelledby="cover-letter-form-title">
                <div className="document-section-heading">
                    <h3 id="cover-letter-form-title">자기소개서 내용</h3>
                    <span>각 항목은 최대 1,000자까지 작성할 수 있습니다.</span>
                </div>
                <div className="document-field-grid">
                    <div className="document-field document-field-wide">
                        <label htmlFor="coverLetterTitle">자기소개서 제목 <b>*</b></label>
                        <input id="coverLetterTitle" name="coverLetterTitle" value={coverLetter.coverLetterTitle} onChange={handleChange} maxLength="200" placeholder="예: 꾸준히 성장하는 개발자 홍길동입니다" required />
                    </div>
                    {coverLetterFields.map((field) => (
                        <div className="document-field document-field-wide" key={field.name}>
                            <div className="document-label-row">
                                <label htmlFor={field.name}>{field.label}</label>
                                <span>{coverLetter[field.name].length} / 1,000</span>
                            </div>
                            <textarea className="document-fixed-textarea" id={field.name} name={field.name} value={coverLetter[field.name]} onChange={handleChange} maxLength="1000" rows="8" placeholder={field.placeholder} />
                        </div>
                    ))}
                </div>
            </section>

            <div className="document-form-actions">
                {onCancel && (
                    <button type="button" className="document-secondary-button" onClick={onCancel} disabled={isSaving}>
                        취소
                    </button>
                )}
                <button type="submit" className="document-primary-button" disabled={isSaving}>
                    {isSaving ? '저장 중...' : `자기소개서 ${isEditMode ? '수정' : '저장'}`}
                </button>
            </div>
            {saveMessage.text && (
                <p className={`document-save-message is-${saveMessage.type}`} role="status">
                    {saveMessage.text}
                </p>
            )}
        </form>
    );
}

export default CoverLetterWrite;
