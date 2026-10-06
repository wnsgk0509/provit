import React, { useState, useEffect } from "react";
import { getAdminReportList, updateAdminReportStatus } from "../../api/adminApi";
import { Link } from "react-router-dom";

const AdminReportList = () => {
  const [reports, setReports] = useState([]);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [statusFilter, setStatusFilter] = useState("");

  const fetchReports = async () => {
    try {
      const data = await getAdminReportList(page, 10, statusFilter);
      if (data.responseCode && data.responseCode.code === 200) {
        setReports(data.data.list || []);
        setTotalPages(data.data.totalPages || 1);
      }
    } catch (error) {
      console.error("신고 목록 조회 실패:", error);
      alert("신고 목록을 불러오는데 실패했습니다.");
    }
  };

  useEffect(() => {
    fetchReports();
  }, [page, statusFilter]);

  const getReasonText = (reasonCode) => {
    const REASON_MAP = {
      SPAM: '스팸/홍보성',
      ABUSE: '욕설/비방',
      ADULT: '음란물',
      ILLEGAL: '불법 정보',
      OTHER: '기타'
    };
    return REASON_MAP[reasonCode] || reasonCode;
  };

  const handleStatusChange = async (reportNum, newStatus) => {
    const isBlind = newStatus === "RESOLVED";
    const confirmMsg = isBlind
      ? "해당 게시물/댓글을 블라인드 처리하시겠습니까?"
      : "해당 신고를 반려 처리하시겠습니까?";

    if (!window.confirm(confirmMsg)) return;

    try {
      const data = await updateAdminReportStatus(reportNum, newStatus);
      if (data.responseCode && data.responseCode.code === 200) {
        alert("처리가 완료되었습니다.");
        fetchReports(); // 목록 새로고침
      } else {
        alert(data.responseCode?.message || "처리에 실패했습니다.");
      }
    } catch (error) {
      console.error("신고 상태 변경 실패:", error);
      alert("서버 오류가 발생했습니다.");
    }
  };

  return (
    <div className="container py-5">
      <h2 className="fw-bold mb-4">관리자 대시보드</h2>

      {/* 관리자 서브 네비게이션 */}
      <ul className="nav nav-tabs mb-4">
        <li className="nav-item">
          <Link to="/admin/users" className="nav-link text-muted">
            회원 관리
          </Link>
        </li>
        <li className="nav-item">
          <Link to="/admin/reports" className="nav-link active fw-bold text-primary">
            신고 관리
          </Link>
        </li>
      </ul>

      {/* 필터 탭 (신고 전용) */}
      <ul className="nav nav-pills mb-4 flex-nowrap text-nowrap justify-content-between w-100">
        <li className="nav-item flex-fill me-1">
          <button
            className={`nav-link w-100 px-1 py-1 text-center ${statusFilter === "" ? "active" : "bg-light text-dark border"}`}
            style={{ fontSize: '0.85rem' }}
            onClick={() => { setStatusFilter(""); setPage(1); }}
          >
            전체
          </button>
        </li>
        <li className="nav-item flex-fill me-1">
          <button
            className={`nav-link w-100 px-1 py-1 text-center ${statusFilter === "PENDING" ? "active" : "bg-light text-dark border"}`}
            style={{ fontSize: '0.85rem' }}
            onClick={() => { setStatusFilter("PENDING"); setPage(1); }}
          >
            대기중
          </button>
        </li>
        <li className="nav-item flex-fill me-1">
          <button
            className={`nav-link w-100 px-1 py-1 text-center ${statusFilter === "RESOLVED" ? "active" : "bg-light text-dark border"}`}
            style={{ fontSize: '0.85rem' }}
            onClick={() => { setStatusFilter("RESOLVED"); setPage(1); }}
          >
            블라인드 완료
          </button>
        </li>
        <li className="nav-item flex-fill">
          <button
            className={`nav-link w-100 px-1 py-1 text-center ${statusFilter === "REJECTED" ? "active" : "bg-light text-dark border"}`}
            style={{ fontSize: '0.85rem' }}
            onClick={() => { setStatusFilter("REJECTED"); setPage(1); }}
          >
            반려됨
          </button>
        </li>
      </ul>

      {/* PC 버전 신고 목록 테이블 */}
      <div className="table-responsive d-none d-md-block">
        <table className="table table-hover align-middle border-top text-nowrap">
          <thead className="table-light">
            <tr>
              <th scope="col" className="text-center">No.</th>
              <th scope="col" className="text-center">구분</th>
              <th scope="col">신고사유 / 원본내용</th>
              <th scope="col" className="text-center">신고자</th>
              <th scope="col" className="text-center">일시</th>
              <th scope="col" className="text-center">상태</th>
              <th scope="col" className="text-center">관리</th>
            </tr>
          </thead>
          <tbody>
            {reports.length === 0 ? (
              <tr>
                <td colSpan="7" className="text-center py-5 text-muted">
                  신고 내역이 없습니다.
                </td>
              </tr>
            ) : (
              reports.map((report) => (
                <tr key={report.reportNum}>
                  <td className="text-center text-muted">{report.reportNum}</td>
                  <td className="text-center">
                    <span className={`badge ${report.targetType === 'POST' ? 'bg-primary' : 'bg-secondary'}`}>
                      {report.targetType === 'POST' ? '게시글' : '댓글'}
                    </span>
                  </td>
                  <td>
                    <div className="fw-bold text-danger mb-1">{getReasonText(report.reportReason)}</div>
                    <div className="text-muted small text-truncate" style={{ maxWidth: '300px' }}>
                      {report.targetType === 'POST' ? (
                        <Link to={`/community/${report.targetNum}`} className="text-decoration-none text-muted" target="_blank">
                          " {report.targetContentPreview || '내용 없음'} ... "
                        </Link>
                      ) : report.commentPostNum ? (
                        <Link to={`/community/${report.commentPostNum}`} className="text-decoration-none text-muted" target="_blank">
                          " {report.targetContentPreview || '내용 없음'} ... "
                        </Link>
                      ) : (
                        <span>" {report.targetContentPreview || '내용 없음'} ... "</span>
                      )}
                    </div>
                  </td>
                  <td className="text-center">{report.reporterNickname || `탈퇴유저(${report.reporterNum})`}</td>
                  <td className="text-center text-muted small">{report.reportDate}</td>
                  <td className="text-center">
                    {report.reportStatus === 'PENDING' && <span className="badge bg-warning text-dark">대기중</span>}
                    {report.reportStatus === 'RESOLVED' && <span className="badge bg-success">블라인드</span>}
                    {report.reportStatus === 'REJECTED' && <span className="badge bg-danger">반려</span>}
                  </td>
                  <td className="text-center">
                    {report.reportStatus === 'PENDING' ? (
                      <div className="btn-group btn-group-sm">
                        <button
                          className="btn btn-outline-danger px-1 py-0 d-flex align-items-center justify-content-center"
                          style={{ fontSize: '0.85rem' }}
                          onClick={() => handleStatusChange(report.reportNum, 'RESOLVED')}
                        >
                          블라인드
                        </button>
                        <button
                          className="btn btn-outline-secondary px-1 py-0 d-flex align-items-center justify-content-center"
                          style={{ fontSize: '0.85rem' }}
                          onClick={() => handleStatusChange(report.reportNum, 'REJECTED')}
                        >
                          반려
                        </button>
                      </div>
                    ) : (
                      <span className="text-muted small">-</span>
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* 모바일 버전 신고 목록 카드 */}
      <div className="d-block d-md-none">
        {reports.length === 0 ? (
          <div className="text-center py-5 text-muted border rounded bg-light">
            신고 내역이 없습니다.
          </div>
        ) : (
          reports.map((report) => (
            <div key={`mobile-${report.reportNum}`} className="card mb-3 shadow-sm border-0">
              <div className="card-body border rounded">
                <div className="d-flex justify-content-between align-items-center mb-2">
                  <span className={`badge ${report.targetType === 'POST' ? 'bg-primary' : 'bg-secondary'}`}>
                    {report.targetType === 'POST' ? '게시글' : '댓글'}
                  </span>
                  <div>
                    {report.reportStatus === 'PENDING' && <span className="badge bg-warning text-dark">대기중</span>}
                    {report.reportStatus === 'RESOLVED' && <span className="badge bg-success">블라인드</span>}
                    {report.reportStatus === 'REJECTED' && <span className="badge bg-danger">반려</span>}
                  </div>
                </div>
                <div className="mb-3">
                  <div className="fw-bold text-danger mb-1">{getReasonText(report.reportReason)}</div>
                  <div className="text-muted small text-truncate" style={{ maxWidth: '100%' }}>
                    {report.targetType === 'POST' ? (
                      <Link to={`/community/${report.targetNum}`} className="text-decoration-none text-muted" target="_blank">
                        " {report.targetContentPreview || '내용 없음'} ... "
                      </Link>
                    ) : report.commentPostNum ? (
                      <Link to={`/community/${report.commentPostNum}`} className="text-decoration-none text-muted" target="_blank">
                        " {report.targetContentPreview || '내용 없음'} ... "
                      </Link>
                    ) : (
                      <span>" {report.targetContentPreview || '내용 없음'} ... "</span>
                    )}
                  </div>
                </div>
                <div className="mb-3 text-muted small">
                  <div><strong>No.</strong> {report.reportNum}</div>
                  <div><strong>신고자:</strong> {report.reporterNickname || `탈퇴유저(${report.reporterNum})`}</div>
                  <div><strong>일시:</strong> {report.reportDate}</div>
                </div>
                {report.reportStatus === 'PENDING' && (
                  <div className="d-flex gap-2 mt-3">
                    <button
                      className="btn btn-outline-danger flex-fill px-1 py-0 d-flex align-items-center justify-content-center"
                      style={{ fontSize: '0.85rem' }}
                      onClick={() => handleStatusChange(report.reportNum, 'RESOLVED')}
                    >
                      블라인드
                    </button>
                    <button
                      className="btn btn-outline-secondary flex-fill px-1 py-0 d-flex align-items-center justify-content-center"
                      style={{ fontSize: '0.85rem' }}
                      onClick={() => handleStatusChange(report.reportNum, 'REJECTED')}
                    >
                      반려
                    </button>
                  </div>
                )}
              </div>
            </div>
          ))
        )}
      </div>

      {/* 페이지네이션 */}
      {totalPages > 1 && (
        <nav className="mt-4">
          <ul className="pagination justify-content-center">
            <li className={`page-item ${page === 1 ? "disabled" : ""}`}>
              <button className="page-link" onClick={() => setPage(page - 1)}>
                이전
              </button>
            </li>
            {Array.from({ length: totalPages }, (_, i) => i + 1).map((num) => (
              <li key={num} className={`page-item ${page === num ? "active" : ""}`}>
                <button className="page-link" onClick={() => setPage(num)}>
                  {num}
                </button>
              </li>
            ))}
            <li className={`page-item ${page === totalPages ? "disabled" : ""}`}>
              <button className="page-link" onClick={() => setPage(page + 1)}>
                다음
              </button>
            </li>
          </ul>
        </nav>
      )}
    </div>
  );
};

export default AdminReportList;
