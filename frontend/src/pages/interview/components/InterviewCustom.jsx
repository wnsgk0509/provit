import { INTERVIEW_DIFFICULTIES } from '../../../constants/interviewDifficulty';
import { Link } from 'react-router-dom';

const DOCUMENT_FIELDS = [
    { type: 'resume', label: '이력서', listKey: 'resumeList', settingKey: 'resumeNum', required: true },
    { type: 'cover-letter', label: '자기소개서', listKey: 'coverLetterList', settingKey: 'letterNum', required: true },
    { type: 'portfolio', label: '포트폴리오', listKey: 'portfolioList', settingKey: 'portfolioNum', required: false },
];

function InterviewDocumentField({ field, documents, settings, onSettingChange }) {
    const list = documents?.[field.listKey] ?? [];
    const isMissing = Boolean(documents) && list.length === 0;
    const inputId = `interview-${field.type}`;

    return (
        <div className="interview-form-group">
            <label htmlFor={isMissing ? undefined : inputId}>
                {field.required && <b>*</b>} {field.label}
            </label>
            {isMissing ? (
                <div className="interview-document-empty">
                    <Link className="interview-document-create" to={`/documents/write?type=${field.type}`}>
                        {field.label} {field.type === 'portfolio' ? '등록하기' : '작성하기'}
                    </Link>
                    <p style={{marginLeft:"auto"}}>저장된 {field.label}가 없습니다.</p>
                    
                </div>
            ) : (
                <select
                    id={inputId}
                    value={settings[field.settingKey]}
                    onChange={(event) => onSettingChange(field.settingKey, event.target.value)}
                    disabled={!documents}
                    required={field.required}
                >
                    <option value="">({field.required ? '필수' : '선택'}) {field.label}를 선택해 주세요</option>
                    {list.map((document) => (
                        <option key={document.documentNum} value={document.documentNum}>
                            {document.documentTitle || `${field.label} #${document.documentNum}`}
                        </option>
                    ))}
                </select>
            )}
        </div>
    );
}

function InterviewCustom({ settings, documents, isLoading, startDisabled = false, onSettingChange, onStart }) {
    const handleSubmit = (event) => {
        event.preventDefault();
        onStart();
    };

    return (
        <section className="interview-custom">
            <div className="interview-section-heading">
                <span>STEP 1</span>
                <h2>면접 커스텀</h2>
                <p>모든 면접은 일대일로 진행됩니다. 사용할 서류와 난이도를 선택해 주세요.</p>
            </div>

            <form onSubmit={handleSubmit}>
                {DOCUMENT_FIELDS.map((field) => (
                    <InterviewDocumentField
                        key={field.type}
                        field={field}
                        documents={documents}
                        settings={settings}
                        onSettingChange={onSettingChange}
                    />
                ))}

                <hr />

                <fieldset className="interview-form-group interview-difficulty" aria-describedby="interview-difficulty-description">
                    <legend><b>*</b> 면접 난이도</legend>
                    <div className="interview-radio-group">
                        {INTERVIEW_DIFFICULTIES.map((difficulty) => (
                            <label key={difficulty.value} htmlFor={`difficulty-${difficulty.value}`}>
                                <input
                                    id={`difficulty-${difficulty.value}`}
                                    type="radio"
                                    name="interview-difficulty"
                                    value={difficulty.value}
                                    checked={settings.difficulty === difficulty.value}
                                    onChange={(event) => onSettingChange('difficulty', event.target.value)}
                                    required
                                />
                                <span>{difficulty.label}</span>
                            </label>
                        ))}
                    </div>
                    <p id="interview-difficulty-description" className="interview-difficulty-description" aria-live="polite">
                        {INTERVIEW_DIFFICULTIES.find((difficulty) => difficulty.value === settings.difficulty)?.description
                            ?? '일반은 기초 확인, 심층은 판단 근거 검증, 압박은 반론과 제약 속 판단을 검증합니다.'}
                    </p>
                </fieldset>

                <button
                    className="btn btn-primary interview-primary-button"
                    type="submit"
                    disabled={isLoading || startDisabled
                        || !documents?.resumeList?.length
                        || !documents?.coverLetterList?.length}
                >
                    {isLoading ? '면접 준비 중...' : '면접 시작'}
                </button>
            </form>
        </section>
    );
}

export default InterviewCustom;
