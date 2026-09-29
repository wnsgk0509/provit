import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { calculateMbti, QUESTIONS } from './mbtiData';
import './Mbti.css';

const INITIAL_SCORES = { E: 0, I: 0, S: 0, N: 0, T: 0, F: 0, J: 0, P: 0 };

function Mbti() {
  const navigate = useNavigate();
  const [questionIndex, setQuestionIndex] = useState(0);
  const [scores, setScores] = useState(INITIAL_SCORES);
  const question = QUESTIONS[questionIndex];

  const selectAnswer = (selectedType) => {
    const nextScores = { ...scores, [selectedType]: scores[selectedType] + 1 };

    if (questionIndex === QUESTIONS.length - 1) {
      navigate(`/mbti/result/${calculateMbti(nextScores)}`);
      return;
    }

    setScores(nextScores);
    setQuestionIndex((current) => current + 1);
  };

  return (
    <div className="mbti-page">
      <section className="mbti-card mbti-fade-in">
        <div className="d-flex justify-content-between align-items-center mb-2">
          <span className="fw-bold text-secondary">Q{questionIndex + 1} / {QUESTIONS.length}</span>
        </div>
        <div className="progress mb-4" aria-label="검사 진행률">
          <div className="progress-bar" style={{ width: `${((questionIndex + 1) / QUESTIONS.length) * 100}%` }} />
        </div>
        <h1 className="h3 fw-bold text-dark mb-4 mbti-question">{question.question}</h1>
        <div className="d-grid gap-3">
          <button type="button" className="mbti-choice" onClick={() => selectAnswer(question.firstType)}>{question.first}</button>
          <button type="button" className="mbti-choice" onClick={() => selectAnswer(question.secondType)}>{question.second}</button>
        </div>
      </section>
    </div>
  );
}

export default Mbti;
