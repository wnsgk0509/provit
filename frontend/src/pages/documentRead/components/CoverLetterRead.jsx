import { DocumentDate } from './ResumeRead';

const coverLetterFields = [
    { name: 'growthProcess', label: '성장 과정' },
    { name: 'personalityStrengthsWeaknesses', label: '성격의 장단점' },
    { name: 'problemSolvingExperience', label: '문제 해결 경험' },
    { name: 'postJoiningAspiration', label: '입사 후 포부' },
];

function CoverLetterRead({ document }) {
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
        </article>
    );
}

export default CoverLetterRead;
