import { useState } from 'react';
import { Link, Navigate, useParams } from 'react-router-dom';
import { Share2 } from 'lucide-react';
import { RESULT_DETAILS, RESULTS } from './mbtiData';
import './Mbti.css';

function MbtiResult() {
  const { mbtiType } = useParams();
  const type = mbtiType?.toUpperCase();
  const guide = RESULTS[type];
  const details = RESULT_DETAILS[type];
  const [shareMessage, setShareMessage] = useState('');

  if (!guide || !details) return <Navigate to="/mbti" replace />;

  const analysis = [...guide.analysis, ...details.analysis];

  const handleShare = async () => {
    const shareUrl = window.location.href;
    const shareData = {
      title: `Provit 취준 MBTI 결과: ${type}`,
      text: `나의 취준 MBTI는 ${type}, ${guide.title}입니다!`,
      url: shareUrl,
    };

    try {
      if (navigator.share) {
        await navigator.share(shareData);
        setShareMessage('결과를 공유했습니다.');
        return;
      }

      await copyToClipboard(shareUrl);
      setShareMessage('결과 링크를 복사했습니다.');
    } catch (error) {
      if (error?.name === 'AbortError') return;

      try {
        await copyToClipboard(shareUrl);
        setShareMessage('공유 창을 열지 못해 링크를 복사했습니다.');
      } catch {
        setShareMessage('링크 복사에 실패했습니다. 주소창의 URL을 복사해 주세요.');
      }
    }
  };

  return (
    <div className="mbti-page">
      <section className="mbti-card mbti-fade-in">
        <div className="text-center mb-4">
          <h2 className="h3 fw-bold text-dark">{guide.title}</h2>
        </div>

        <img className="mbti-result-image" src={`/images/mbti/${type}.jpg`} alt={`${type} ${guide.animal} 캐릭터`} />

        <article className="mbti-result-box mb-3">
          <h3 className="h5 fw-bold">취준 성향 분석</h3>
          <ol className="mbti-analysis-list">
            {analysis.map((item) => <li key={item}>{item}</li>)}
          </ol>
        </article>

        <div className="row g-3 mb-3">
          <div className="col-md-6">
            <article className="mbti-detail-box h-100">
              <h3 className="h6 fw-bold text-primary">추천하는 직업</h3>
              {guide.jobs.map((item) => <p key={item} className="mb-0 text-secondary small">{item}</p>)}
            </article>
          </div>
          <div className="col-md-6">
            <article className="mbti-detail-box h-100">
              <h3 className="h6 fw-bold text-warning-emphasis">안 맞는 직업</h3>
              {guide.avoid.map((item) => <p key={item} className="mb-0 text-secondary small">{item}</p>)}
            </article>
          </div>
        </div>

        <section className="mbti-compatibility mb-3" aria-label="MBTI 궁합">
          <div>
            <h3>잘 맞는 유형</h3>
            <div className="mbti-type-tags">
              {details.compatible.map((mbti) => (
                <Link key={mbti} to={`/mbti/result/${mbti}`} className="is-compatible">
                  {mbti}
                </Link>
              ))}
            </div>
          </div>
          <div>
            <h3>안 맞는 유형</h3>
            <div className="mbti-type-tags">
              {details.incompatible.map((mbti) => (
                <Link key={mbti} to={`/mbti/result/${mbti}`} className="is-incompatible">
                  {mbti}
                </Link>
              ))}
            </div>
          </div>
        </section>

        <article className="alert alert-primary border-0 mb-4">
          <h3 className="h6 fw-bold">맞춤 조언</h3>
          <p className="mb-0 small">{guide.advice}</p>
        </article>

        <div className="d-grid gap-2">
          <button type="button" className="btn mbti-share-button" onClick={handleShare}>
            <Share2 size={17} aria-hidden="true" /> 결과 공유하기
          </button>
          {shareMessage && <p className="mbti-share-message" role="status" aria-live="polite">{shareMessage}</p>}
          <Link className="btn mbti-primary-button" to="/mbti">다시 검사하기</Link>
          <Link className="btn btn-outline-secondary border-0" to="/fortune">오늘의 운세로 돌아가기</Link>
        </div>
      </section>
    </div>
  );
}

async function copyToClipboard(value) {
  if (navigator.clipboard?.writeText && window.isSecureContext) {
    await navigator.clipboard.writeText(value);
    return;
  }

  const textarea = document.createElement('textarea');
  textarea.value = value;
  textarea.setAttribute('readonly', '');
  textarea.style.position = 'fixed';
  textarea.style.opacity = '0';
  document.body.appendChild(textarea);
  textarea.select();
  const copied = document.execCommand('copy');
  textarea.remove();

  if (!copied) throw new Error('Clipboard copy failed');
}

export default MbtiResult;
