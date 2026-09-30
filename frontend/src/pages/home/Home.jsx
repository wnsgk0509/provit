import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { fetchPostList } from "../../api/communityApi";
import { getInterviewResults } from "../../api/interviewApi";
import { useAuth } from "../../context/AuthContext";
import "./Home.css";

const quickLinks = [
  { code: "DOC", title: "지원 문서 첨삭", description: "이력서·자소서·포트폴리오를 관리하고 개선점을 확인합니다.", to: "/documents/write", requiresLogin: true },
  { code: "Q5", title: "맞춤 모의면접", description: "내 문서를 바탕으로 생성된 질문에 답하며 실전 감각을 키웁니다.", to: "/interview", requiresLogin: true },
  { code: "TALK", title: "취업 커뮤니티", description: "질문과 합격 후기를 나누고 스터디를 함께 만들어 보세요.", to: "/community" },
  { code: "LUCK", title: "오늘의 운세", description: "오늘의 취업·면접 운세와 MBTI 콘텐츠를 확인합니다.", to: "/fortune" },
];

const formatDate = (value) => {
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? ""
    : new Intl.DateTimeFormat("ko-KR", { month: "numeric", day: "numeric" }).format(date);
};

function Home() {
  const navigate = useNavigate();
  const { isLoggedIn, user } = useAuth();
  const [interviewResults, setInterviewResults] = useState([]);
  const [posts, setPosts] = useState([]);
  const [isLoadingResults, setIsLoadingResults] = useState(false);
  const [isLoadingPosts, setIsLoadingPosts] = useState(true);

  useEffect(() => {
    const loadPosts = async () => {
      try {
        const response = await fetchPostList({ page: 1, pageSize: 3, searchType: "TITLE", keyword: "" });
        setPosts(response?.data?.list ?? []);
      } catch {
        setPosts([]);
      } finally {
        setIsLoadingPosts(false);
      }
    };

    loadPosts();
  }, []);

  useEffect(() => {
    if (!isLoggedIn) return undefined;

    let isMounted = true;
    const loadResults = async () => {
      setIsLoadingResults(true);
      try {
        const results = await getInterviewResults();
        if (isMounted) setInterviewResults(Array.isArray(results) ? results : []);
      } catch {
        if (isMounted) setInterviewResults([]);
      } finally {
        if (isMounted) setIsLoadingResults(false);
      }
    };

    loadResults();
    return () => {
      isMounted = false;
    };
  }, [isLoggedIn]);

  const sortedResults = useMemo(
    () => [...interviewResults].sort((a, b) => new Date(a.interviewDate) - new Date(b.interviewDate)),
    [interviewResults],
  );
  const latestResult = isLoggedIn ? sortedResults[sortedResults.length - 1] : null;
  const previousResult = isLoggedIn ? sortedResults[sortedResults.length - 2] : null;
  const chartResults = sortedResults.slice(-5);
  const scoreDelta = latestResult && previousResult
    ? Math.round(latestResult.totalScore - previousResult.totalScore)
    : null;
  const chartPoints = useMemo(() => chartResults.map((result, index) => {
    const score = Math.max(0, Math.min(100, Number(result.totalScore) || 0));
    const x = chartResults.length === 1 ? 210 : 16 + (388 / (chartResults.length - 1)) * index;
    return { x, y: 120 - score * 0.9, score };
  }), [chartResults]);
  const chartLine = chartPoints.map(({ x, y }) => `${x},${y}`).join(" ");
  const chartArea = chartPoints.length ? `${chartLine} ${chartPoints.at(-1).x},132 ${chartPoints[0].x},132` : "";

  const userName = user?.userNickname || user?.userName || "회원";
  const heroTitle = !isLoggedIn
    ? "모의면접으로\n취업 준비를 점검해 보세요."
    : latestResult
      ? `${userName}님의 최근 면접 결과를\n확인해 보세요.`
      : `${userName}님, 첫 모의면접을\n시작해 보세요.`;
  const heroDescription = !isLoggedIn
    ? "문서 첨삭부터 실전형 면접 연습까지, 취업 준비 과정을 한곳에서 관리할 수 있습니다."
    : latestResult
      ? "최근 면접 결과를 바탕으로 강점과 보완할 점을 점검하고 다음 면접을 준비하세요."
      : "내 문서를 바탕으로 질문을 받고, 답변에 대한 피드백과 점수를 확인해 보세요.";

  return (
    <section className="home-page" aria-labelledby="homeTitle">
      <div className={`home-hero ${isLoggedIn ? "" : "is-guest"}`}>
        <div className="home-hero-copy">
          <div className="home-eyebrow">{isLoggedIn ? "MY INTERVIEW DASHBOARD" : "PROVIT CAREER COACHING"}</div>
          <h1 id="homeTitle">{heroTitle.split("\n").map((line) => <span key={line}>{line}</span>)}</h1>
          <p>{heroDescription}</p>
          <div className="home-hero-actions">
            <button type="button" className="home-button home-button-lime" onClick={() => navigate(isLoggedIn ? "/interview" : "/login")}>
              {isLoggedIn ? "모의면접 시작" : "로그인하고 시작하기"}
            </button>
            {isLoggedIn && <Link className="home-button home-button-ghost" to="/mypage">내 자료 관리</Link>}
          </div>
        </div>

        {isLoggedIn && <article className="home-score-card" aria-live="polite">
          {isLoadingResults ? <p className="home-card-message">최근 면접 결과를 불러오는 중입니다.</p> : latestResult ? (
            <>
              <div className="home-score-top">
                <div><div className="home-subtle">최근 모의면접 총점</div><div><span className="home-score-number">{Math.round(latestResult.totalScore)}</span><strong>/100</strong></div></div>
                {scoreDelta !== null && <span className={`home-delta ${scoreDelta < 0 ? "is-down" : ""}`}>직전 대비 {scoreDelta > 0 ? "+" : ""}{scoreDelta}</span>}
              </div>
              <div className="home-chart-wrap">
                <svg viewBox="0 0 420 145" role="img" aria-label="최근 모의면접 점수 추이 그래프">
                  <defs><linearGradient id="homeScoreGradient" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stopColor="#6f8cff" stopOpacity="0.55" /><stop offset="1" stopColor="#6f8cff" stopOpacity="0" /></linearGradient></defs>
                  <path className="home-chart-grid" d="M10 30H410M10 75H410M10 120H410" />
                  {chartPoints.length > 1 && <polygon className="home-chart-area" points={chartArea} />}
                  {chartPoints.length > 1 && <polyline className="home-chart-line" points={chartLine} />}
                  {chartPoints.map(({ x, y, score }) => <circle key={`${x}-${score}`} className="home-chart-dot" cx={x} cy={y} r="5" />)}
                </svg>
              </div>
              <div className="home-score-legend">{chartResults.map((result, index) => <span key={result.historyNum || index}>{index + 1}회 {Math.round(result.totalScore)}점</span>)}</div>
            </>
          ) : (
            <div className="home-empty-score"><strong>아직 면접 결과가 없어요.</strong><p>첫 모의면접을 완료하면 점수와 성장 추이를 이곳에서 확인할 수 있습니다.</p><Link to={isLoggedIn ? "/interview" : "/login"}>첫 모의면접 시작하기 →</Link></div>
          )}
        </article>}
      </div>

      <div className="home-section-heading"><div><h2>필요한 기능을 바로 시작하세요</h2><p>취업 준비에 자주 쓰는 기능을 빠르게 이어갑니다.</p></div></div>
      <div className="home-quick-grid">
        {quickLinks.map((item) => <Link className="home-quick-card" to={item.requiresLogin && !isLoggedIn ? "/login" : item.to} key={item.title}><span className="home-quick-icon">{item.code}</span><h3>{item.title}</h3><p>{item.description}</p><span className="home-quick-arrow" aria-hidden="true">→</span></Link>)}
      </div>

      <section className="home-community" aria-labelledby="latestPostsTitle">
        <div className="home-panel-heading"><div><div className="home-eyebrow">COMMUNITY</div><h2 id="latestPostsTitle">커뮤니티 최신글</h2></div><Link className="home-text-link" to="/community">전체 보기 →</Link></div>
        {isLoadingPosts ? <p className="home-card-message">최신글을 불러오는 중입니다.</p> : posts.length ? (
          <div className="home-post-list">{posts.map((post) => <Link className="home-post-row" to={`/community/${post.postNum}`} key={post.postNum}><span className="home-post-category">{post.categoryName || "커뮤니티"}</span><strong>{post.postTitle}</strong><span className="home-post-writer">{post.userNickname || "익명"}</span><span className="home-post-date">{formatDate(post.postDate)}</span></Link>)}</div>
        ) : <p className="home-card-message">아직 등록된 커뮤니티 글이 없습니다.</p>}
      </section>
    </section>
  );
}

export default Home;
