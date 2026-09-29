import { Link, Navigate, useParams } from 'react-router-dom';
import { RESULTS } from './mbtiData';
import './Mbti.css';

function MbtiResult() {
  const { mbtiType } = useParams();
  const type = mbtiType?.toUpperCase();
  const guide = RESULTS[type];

  if (!guide) return <Navigate to="/mbti" replace />;

  return (
    <div className="mbti-page">
      <section className="mbti-card mbti-fade-in">
        <div className="text-center mb-4">
          <h2 className="h3 fw-bold text-dark">{guide.title}</h2>
        </div>

        <img className="mbti-result-image" src={`/images/mbti/${type}.jpg`} alt={`${type} ${guide.animal} 캐릭터`} />
        <section className="mbti-character-box mb-4">
          <h3 className="h6 fw-bold">캐릭터 컨셉</h3>
          <p className="mbti-character-quote">{guide.quote}</p>
          <p className="mb-2 text-secondary small"><strong>비주얼:</strong> {guide.visual}</p>
          <p className="mb-0 text-secondary small"><strong>한 줄 대사:</strong> {guide.line}</p>
        </section>

        <article className="mbti-result-box mb-3">
          <h3 className="h5 fw-bold">취준 성향 분석</h3>
          {guide.analysis.map((item) => <p key={item} className="mb-2 text-secondary">{item}</p>)}
        </article>

        <div className="row g-3 mb-3">
          <div className="col-md-6"><article className="mbti-detail-box h-100"><h3 className="h6 fw-bold text-primary">잘 어울리는 직무</h3>{guide.jobs.map((item) => <p key={item} className="mb-0 text-secondary small">{item}</p>)}</article></div>
          <div className="col-md-6"><article className="mbti-detail-box h-100"><h3 className="h6 fw-bold text-warning-emphasis">안 맞는 취준 환경 & 직무</h3>{guide.avoid.map((item) => <p key={item} className="mb-0 text-secondary small">{item}</p>)}</article></div>
        </div>
        <article className="alert alert-primary border-0 mb-4"><h3 className="h6 fw-bold">맞춤 조언</h3><p className="mb-0 small">{guide.advice}</p></article>
        <div className="d-grid gap-2">
          <Link className="btn mbti-primary-button" to="/mbti">다시 검사하기</Link>
          <Link className="btn btn-outline-secondary border-0" to="/fortune">오늘의 운세로 돌아가기</Link>
        </div>
      </section>
    </div>
  );
}

export default MbtiResult;
