import './MainResumeFeedback.css';

export default function MainResumeFeedback({ state }) {
    return (
        <div className="main-resume-feedback">
            {state.loading ? (
                <p role="status">대표이력서를 확인하고 있습니다.</p>
            ) : state.loadError ? (
                <div className="main-resume-load-error" role="alert">
                    <span>{state.loadError}</span>
                    <button type="button" onClick={state.reload}>다시 시도</button>
                </div>
            ) : state.mainResumeNum == null ? (
                <p>공고검색에 사용할 이력서를 대표로 지정해 주세요.</p>
            ) : null}
            {state.message && <p className="main-resume-success" role="status">{state.message}</p>}
            {state.saveError && <p className="main-resume-error" role="alert">{state.saveError}</p>}
        </div>
    );
}
