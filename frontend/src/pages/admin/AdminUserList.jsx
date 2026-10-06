import React, { useState, useEffect } from "react";
import { getAdminUserList, updateAdminUserStatus } from "../../api/adminApi";
import { Link } from "react-router-dom";

const AdminUserList = () => {
  const [users, setUsers] = useState([]);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [statusFilter, setStatusFilter] = useState("");
  const [keyword, setKeyword] = useState("");
  const [searchInput, setSearchInput] = useState("");

  const fetchUsers = async () => {
    try {
      const data = await getAdminUserList(page, 10, keyword, statusFilter);
      if (data.responseCode && data.responseCode.code === 200) {
        setUsers(data.data.list || []);
        setTotalPages(data.data.totalPages || 1);
      }
    } catch (error) {
      console.error("회원 목록 조회 실패:", error);
      alert("회원 목록을 불러오는데 실패했습니다.");
    }
  };

  useEffect(() => {
    fetchUsers();
  }, [page, statusFilter, keyword]);

  const handleSearch = (e) => {
    e.preventDefault();
    setKeyword(searchInput);
    setPage(1);
  };

  const handleStatusChange = async (userNum, currentBlockedDate) => {
    const isCurrentlyBlocked = currentBlockedDate && new Date(currentBlockedDate) > new Date();
    
    let blockDays = 0;
    if (!isCurrentlyBlocked) {
      const action = window.prompt(
        "정지 기간을 선택하세요.\n7 (7일 정지)\n30 (30일 정지)\n9999 (무기한 정지)",
        "7"
      );
      if (action === null) return;
      blockDays = parseInt(action, 10);
      if (![7, 30, 9999].includes(blockDays)) {
        alert("올바른 정지 기간을 입력해주세요 (7, 30, 9999 중 택1)");
        return;
      }
    } else {
      if (!window.confirm("이 회원의 정지 상태를 해제하시겠습니까?")) return;
      blockDays = 0;
    }

    try {
      const data = await updateAdminUserStatus(userNum, blockDays);
      if (data.responseCode && data.responseCode.code === 200) {
        alert("계정 상태가 변경되었습니다.");
        fetchUsers();
      } else {
        alert(data.responseCode?.message || "상태 변경에 실패했습니다.");
      }
    } catch (error) {
      console.error("상태 변경 실패:", error);
      alert("서버 오류가 발생했습니다.");
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return "-";
    const date = new Date(dateString);
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  };

  const isBlocked = (blockedDate) => {
    if (!blockedDate) return false;
    return new Date(blockedDate) > new Date();
  };

  return (
    <div className="container py-5">
      <h2 className="fw-bold mb-4">관리자 대시보드</h2>

      {/* 관리자 서브 네비게이션 */}
      <ul className="nav nav-tabs mb-4">
        <li className="nav-item">
          <Link to="/admin/users" className="nav-link active fw-bold text-primary">
            회원 관리
          </Link>
        </li>
        <li className="nav-item">
          <Link to="/admin/reports" className="nav-link text-muted">
            신고 관리
          </Link>
        </li>
      </ul>

      <div className="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3 mb-4">
        {/* 필터 탭 */}
        <ul className="nav nav-pills flex-nowrap text-nowrap justify-content-between w-100">
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
              className={`nav-link w-100 px-1 py-1 text-center ${statusFilter === "ACTIVE" ? "active" : "bg-light text-dark border"}`} 
              style={{ fontSize: '0.85rem' }}
              onClick={() => { setStatusFilter("ACTIVE"); setPage(1); }}
            >
              정상 회원
            </button>
          </li>
          <li className="nav-item flex-fill">
            <button 
              className={`nav-link w-100 px-1 py-1 text-center ${statusFilter === "BLOCKED" ? "active" : "bg-light text-dark border"}`} 
              style={{ fontSize: '0.85rem' }}
              onClick={() => { setStatusFilter("BLOCKED"); setPage(1); }}
            >
              정지된 회원
            </button>
          </li>
        </ul>

        {/* 검색 폼 */}
        <form onSubmit={handleSearch} className="d-flex mt-2 mt-md-0" style={{ width: '100%', maxWidth: '350px' }}>
          <input
            type="text"
            className="form-control form-control-sm me-2 flex-grow-1"
            placeholder="이메일 또는 닉네임"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            style={{ fontSize: '0.85rem' }}
          />
          <button type="submit" className="btn btn-primary btn-sm text-nowrap px-3" style={{ fontSize: '0.85rem' }}>검색</button>
        </form>
      </div>

      {/* PC 버전 회원 목록 테이블 */}
      <div className="table-responsive d-none d-md-block">
        <table className="table table-hover align-middle border-top text-nowrap">
          <thead className="table-light">
            <tr>
              <th scope="col" className="text-center">회원번호</th>
              <th scope="col">이메일</th>
              <th scope="col" className="text-center">이름(닉네임)</th>
              <th scope="col" className="text-center">가입일</th>
              <th scope="col" className="text-center">상태</th>
              <th scope="col" className="text-center">관리</th>
            </tr>
          </thead>
          <tbody>
            {users.length === 0 ? (
              <tr>
                <td colSpan="6" className="text-center py-5 text-muted">
                  조건에 맞는 회원이 없습니다.
                </td>
              </tr>
            ) : (
              users.map((user) => {
                const blocked = isBlocked(user.blockedDate);
                const isWithdrawn = user.userIsDeleted === 1;

                return (
                  <tr key={user.userNum}>
                    <td className="text-center">{user.userNum}</td>
                    <td>{user.userEmail}</td>
                    <td className="text-center">
                      {user.userName} <br />
                      <span className="text-muted small">({user.userNickname})</span>
                    </td>
                    <td className="text-center">{formatDate(user.userRegisterDate)}</td>
                    <td className="text-center">
                      {isWithdrawn ? (
                        <span className="badge bg-secondary">탈퇴</span>
                      ) : blocked ? (
                        <div>
                          <span className="badge bg-danger mb-1">정지됨</span><br/>
                          <span className="text-danger small">~{formatDate(user.blockedDate)}</span>
                        </div>
                      ) : (
                        <span className="badge bg-success">정상</span>
                      )}
                    </td>
                    <td className="text-center">
                      <button
                        className={`btn btn-sm px-1 py-0 d-flex align-items-center justify-content-center mx-auto ${blocked ? "btn-outline-success" : "btn-outline-danger"}`}
                        style={{ fontSize: '0.85rem', width: '70px' }}
                        onClick={() => handleStatusChange(user.userNum, user.blockedDate)}
                        disabled={isWithdrawn}
                      >
                        {blocked ? "정지 해제" : "계정 정지"}
                      </button>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {/* 모바일 버전 회원 목록 카드 */}
      <div className="d-block d-md-none">
        {users.length === 0 ? (
          <div className="text-center py-5 text-muted border rounded bg-light">
            조건에 맞는 회원이 없습니다.
          </div>
        ) : (
          users.map((user) => {
            const blocked = isBlocked(user.blockedDate);
            const isWithdrawn = user.userIsDeleted === 1;

            return (
              <div key={`mobile-${user.userNum}`} className="card mb-3 shadow-sm border-0">
                <div className="card-body border rounded">
                  <div className="d-flex justify-content-between align-items-center mb-2">
                    <h6 className="card-title mb-0 fw-bold">
                      {user.userName} <span className="text-muted small">({user.userNickname})</span>
                    </h6>
                    <div>
                      {isWithdrawn ? (
                        <span className="badge bg-secondary">탈퇴</span>
                      ) : blocked ? (
                        <span className="badge bg-danger">정지됨</span>
                      ) : (
                        <span className="badge bg-success">정상</span>
                      )}
                    </div>
                  </div>
                  <div className="mb-2 text-muted small">
                    <div><strong>회원번호:</strong> {user.userNum}</div>
                    <div><strong>이메일:</strong> {user.userEmail}</div>
                    <div><strong>가입일:</strong> {formatDate(user.userRegisterDate)}</div>
                    {blocked && (
                      <div className="text-danger mt-1">
                        정지 기한: ~{formatDate(user.blockedDate)}
                      </div>
                    )}
                  </div>
                  <div className="text-end mt-3">
                    <button
                      className={`btn btn-sm w-100 px-1 py-0 d-flex align-items-center justify-content-center ${blocked ? "btn-outline-success" : "btn-outline-danger"}`}
                      style={{ fontSize: '0.85rem' }}
                      onClick={() => handleStatusChange(user.userNum, user.blockedDate)}
                      disabled={isWithdrawn}
                    >
                      {blocked ? "정지 해제" : "계정 정지"}
                    </button>
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* 페이지네이션 */}
      {totalPages > 1 && (
        <nav className="mt-4">
          <ul className="pagination justify-content-center">
            <li className={`page-item ${page === 1 ? "disabled" : ""}`}>
              <button className="page-link" onClick={() => setPage(p => Math.max(1, p - 1))}>
                이전
              </button>
            </li>
            {[...Array(totalPages)].map((_, i) => (
              <li key={i + 1} className={`page-item ${page === i + 1 ? "active" : ""}`}>
                <button className="page-link" onClick={() => setPage(i + 1)}>
                  {i + 1}
                </button>
              </li>
            ))}
            <li className={`page-item ${page === totalPages ? "disabled" : ""}`}>
              <button className="page-link" onClick={() => setPage(p => Math.min(totalPages, p + 1))}>
                다음
              </button>
            </li>
          </ul>
        </nav>
      )}
    </div>
  );
};

export default AdminUserList;
