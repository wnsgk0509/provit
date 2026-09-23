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
                    <label htmlFor="interview-resume"> <b>*</b> 이력서</label>
                    <select
                        id="interview-resume"
                        value={settings.resumeNum}
                        onChange={(event) => onSettingChange('resumeNum', event.target.value)}
                        required
                    >
                        <option value="">(필수) 이력서를 선택해 주세요</option>
                        {(documents?.resumeList ?? []).map((resume) => (
                            <option key={resume.documentNum} value={resume.documentNum}>
                                {resume.documentTitle || `이력서 #${resume.documentNum}`}
                            </option>
                        ))}
                    </select>
                    {documents && !documents.resumeList?.length && <p>저장된 이력서가 없습니다.</p>}
                </div>

                <div className="interview-form-group">
                    <label htmlFor="interview-cover-letter"><b>*</b> 자기소개서 </label>
                    <select
                        id="interview-cover-letter"
                        value={settings.letterNum}
                        onChange={(event) => onSettingChange('letterNum', event.target.value)}
                        disabled={!documents?.coverLetterList?.length}
                        required
                    >
                        <option value="">(필수) 자기소개서를 선택해 주세요</option>
                        {(documents?.coverLetterList ?? []).map((coverLetter) => (
                            <option key={coverLetter.documentNum} value={coverLetter.documentNum}>
                                {coverLetter.documentTitle || `자기소개서 #${coverLetter.documentNum}`}
                            </option>
                        ))}
                    </select>
                    {documents && !documents.coverLetterList?.length && <p>저장된 자기소개서가 없습니다.</p>}
                </div>

                <div className="interview-form-group">
                    <label htmlFor="interview-portfolio">포트폴리오</label>
                    <select
                        id="interview-portfolio"
                        value={settings.portfolioNum}
                        onChange={(event) => onSettingChange('portfolioNum', event.target.value)}
                        disabled={!documents?.portfolioList?.length}
                    >
                        <option value="">(선택) 포트폴리오를 선택해 주세요</option>
                        {(documents?.portfolioList ?? []).map((portfolio) => (
                            <option key={portfolio.documentNum} value={portfolio.documentNum}>
                                {portfolio.documentTitle || `포트폴리오 #${portfolio.documentNum}`}
                            </option>
                        ))}
                    </select>
                    {documents && !documents.portfolioList?.length && <p>저장된 포트폴리오가 없습니다.</p>}
                </div>

                <div className="interview-form-group">
                    <label htmlFor="interview-style"><b>*</b> 면접 스타일</label>
                    <select
                        id="interview-style"
                        value={settings.interviewStyle}
                        onChange={(event) => onSettingChange('interviewStyle', event.target.value)}
                        required
                    >
                        <option value="">(필수) 면접 스타일을 선택해 주세요</option>
                        {INTERVIEW_STYLES.map((style) => (
                            <option key={style.value} value={style.value}>{style.label}</option>
                        ))}
                    </select>
                </div>

                <hr />

                <fieldset className="interview-form-group interview-difficulty">
                    <legend><b>*</b> 면접 난이도</legend>
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

                <button
                    className="btn btn-primary interview-primary-button"
                    type="submit"
                    disabled={isLoading
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
