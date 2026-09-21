const INTERVIEW_STYLES = [
    { value: 'RANDOM', label: '랜덤면접' },
    { value: 'ONE_TO_ONE', label: '일대일면접' },
    { value: 'PANEL', label: '다대일면접' },
    { value: 'GROUP', label: '다대다면접' },
];

const DIFFICULTIES = [
    { value: 'HARD', label: '압박면접' },
    { value: 'NORMAL', label: '심층면접' },
    { value: 'EASY', label: '일반면접' },
];

function InterviewCustom({ settings, documents, isLoading, onSettingChange, onStart }) {
    const handleSubmit = (event) => {
        event.preventDefault();
        onStart();
    };

    return (
        <section className="interview-custom">
            <div className="interview-section-heading">
                <span>STEP 1</span>
                <h2>면접 커스텀</h2>
                <p>면접에 사용할 서류와 진행 방식을 선택해 주세요.</p>
            </div>

            <form onSubmit={handleSubmit}>
                <div className="interview-form-group">
                    <label htmlFor="interview-resume">이력서</label>
                    <select
                        id="interview-resume"
                        value={settings.resumeNum}
                        onChange={(event) => onSettingChange('resumeNum', event.target.value)}
                        required
                    >
                        <option value="">이력서를 선택해 주세요</option>
                        {(documents?.resumeList ?? []).map((resume) => (
                            <option key={resume.resumeNum} value={resume.resumeNum}>
                                {resume.resumeTitle || `이력서 #${resume.resumeNum}`}
                            </option>
                        ))}
                    </select>
                    {documents && !documents.resumeList?.length && <p>저장된 이력서가 없습니다.</p>}
                </div>

                <div className="interview-form-group">
                    <label htmlFor="interview-cover-letter">
                        <input id="interview-cover-letter" type="checkbox" checked={settings.useCoverLetter}
                            disabled={!documents?.coverLetter}
                            onChange={(event) => onSettingChange('useCoverLetter', event.target.checked)} />
                        {' '}{documents?.coverLetter?.coverLetterTitle || '자기소개서 없음'}
                    </label>
                </div>

                <div className="interview-form-group">
                    <label htmlFor="interview-portfolio">
                        <input id="interview-portfolio" type="checkbox" checked={settings.usePortfolio}
                            disabled={!documents?.portfolio}
                            onChange={(event) => onSettingChange('usePortfolio', event.target.checked)} />
                        {' '}{documents?.portfolio?.portfolioTitle || '포트폴리오 없음'}
                    </label>
                </div>

                <div className="interview-form-group">
                    <label htmlFor="interview-style">면접 스타일</label>
                    <select
                        id="interview-style"
                        value={settings.interviewStyle}
                        onChange={(event) => onSettingChange('interviewStyle', event.target.value)}
                        required
                    >
                        <option value="">면접 스타일을 선택해 주세요</option>
                        {INTERVIEW_STYLES.map((style) => (
                            <option key={style.value} value={style.value}>{style.label}</option>
                        ))}
                    </select>
                </div>

                <fieldset className="interview-form-group interview-difficulty">
                    <legend>면접 난이도</legend>
                    <div className="interview-radio-group">
                        {DIFFICULTIES.map((difficulty) => (
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
                </fieldset>

                <button className="btn btn-primary interview-primary-button" type="submit" disabled={isLoading || !documents?.resumeList?.length}>
                    {isLoading ? '면접 준비 중...' : '면접 시작'}
                </button>
            </form>
        </section>
    );
}

export default InterviewCustom;
