import { useId } from 'react';
import { SCORE_ITEMS, formatScore, radarPoint, radarPolygon } from './interviewScores';

function InterviewRadar({ first, latest }) {
    const titleId = useId();
    const descriptionId = useId();
    return (
        <div className="mypage-radar">
            <svg viewBox="0 0 400 350" role="img" aria-labelledby={`${titleId} ${descriptionId}`}>
                <title id={titleId}>최초 면접과 최근 면접의 다섯 가지 역량 비교</title>
                <desc id={descriptionId}>각 항목은 100점 만점입니다. 최초 면접은 주황색 점선, 최근 면접은 파란색 실선입니다. 항목별 점수는 아래 표에서 확인할 수 있습니다.</desc>
                {[20, 40, 60, 80, 100].map((level) => (
                    <g key={level}>
                        <polygon points={SCORE_ITEMS.map((_, index) => radarPoint(index, level).join(',')).join(' ')} className="mypage-radar-grid" />
                        <text x="207" y={180 - level * 1.1 + 4} className="mypage-radar-tick">{level}</text>
                    </g>
                ))}
                {SCORE_ITEMS.map((item, index) => {
                    const [x, y] = radarPoint(index, 100);
                    const [labelX, labelY] = radarPoint(index, 100, 151);
                    return (
                        <g key={item.key}>
                            <line x1="200" y1="180" x2={x} y2={y} className="mypage-radar-axis" />
                            <text x={labelX} y={labelY} dominantBaseline="middle" textAnchor="middle" className="mypage-radar-label">{item.label}</text>
                        </g>
                    );
                })}
                <polygon points={radarPolygon(latest)} className="mypage-radar-latest" />
                <polygon points={radarPolygon(first)} className="mypage-radar-first" />
                {SCORE_ITEMS.map((item, index) => {
                    const [x, y] = radarPoint(index, latest[item.key]);
                    return <circle key={item.key} cx={x} cy={y} r="3.5" className="mypage-radar-dot" />;
                })}
            </svg>
            <div className="mypage-radar-legend">
                <span><i className="is-first" /> 최초 면접 · 점선</span>
                <span><i className="is-latest" /> 최근 면접 · 실선</span>
            </div>
            <table className="mypage-score-table">
                <caption className="mypage-sr-only">최초 면접과 최근 면접의 항목별 점수, 100점 만점</caption>
                <thead><tr><th scope="col">역량</th><th scope="col">최초</th><th scope="col">최근</th></tr></thead>
                <tbody>{SCORE_ITEMS.map((item) => (
                    <tr key={item.key}><th scope="row">{item.label}</th><td>{formatScore(first[item.key])}</td><td>{formatScore(latest[item.key])}</td></tr>
                ))}</tbody>
            </table>
        </div>
    );
}

export default InterviewRadar;
