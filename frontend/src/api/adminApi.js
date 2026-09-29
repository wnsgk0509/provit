import client from "./client";

/**
 * [관리자] 신고 목록 페이징 조회
 * @param {number} page 
 * @param {number} size 
 * @param {string} status 'PENDING', 'RESOLVED', 'REJECTED' (없으면 전체)
 */
export const getAdminReportList = async (page = 1, size = 10, status = "") => {
  const response = await client.get("/admin/reports", {
    params: { page, size, status },
  });
  return response.data;
};

/**
 * [관리자] 신고 상태 변경 (블라인드/반려)
 * @param {number} reportNum 
 * @param {string} reportStatus 'RESOLVED' or 'REJECTED'
 */
export const updateAdminReportStatus = async (reportNum, reportStatus) => {
  const response = await client.put(`/admin/reports/${reportNum}`, {
    reportStatus,
  });
  return response.data;
};
