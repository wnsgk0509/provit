import React from 'react';
import { ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight } from 'lucide-react';
import './Pagination.css';

/**
 * 📄 Provit 공통 모던 페이지네이션 컴포넌트
 *
 * @param {number} currentPage 현재 페이지 (1-based)
 * @param {number} totalPages 전체 페이지 수
 * @param {function} onPageChange 페이지 변경 핸들러
 * @param {number} [blockSize=5] 한 번에 표시할 페이지 번호 개수
 * @param {boolean} [showFirstLast=true] 처음/마지막 이동 버튼 노출 여부
 * @param {string} [className=""] 추가 CSS 클래스
 */
export function Pagination({
  currentPage = 1,
  totalPages = 1,
  onPageChange,
  blockSize = 5,
  showFirstLast = true,
  className = '',
}) {
  if (!totalPages || totalPages <= 1) return null;

  // 블록 단위 계산
  const currentBlock = Math.ceil(currentPage / blockSize);
  const startPage = (currentBlock - 1) * blockSize + 1;
  const endPage = Math.min(startPage + blockSize - 1, totalPages);

  const hasPrevious = currentPage > 1;
  const hasNext = currentPage < totalPages;
  const hasPrevBlock = startPage > 1;
  const hasNextBlock = endPage < totalPages;

  const pageNumbers = [];
  for (let i = startPage; i <= endPage; i++) {
    pageNumbers.push(i);
  }

  const handlePageClick = (page) => {
    if (page === currentPage || page < 1 || page > totalPages) return;
    onPageChange?.(page);
  };

  return (
    <nav className={`provit-pagination-container ${className}`} aria-label="Page navigation">
      <ul className="provit-pagination-list">
        {/* 맨 처음 페이지로 이동 */}
        {showFirstLast && (
          <li>
            <button
              type="button"
              className="provit-page-btn provit-page-nav-btn"
              onClick={() => handlePageClick(1)}
              disabled={!hasPrevious}
              aria-label="첫 페이지로 이동"
              title="첫 페이지"
            >
              <ChevronsLeft size={16} strokeWidth={2.2} />
            </button>
          </li>
        )}

        {/* 이전 페이지 블록으로 이동 (<) */}
        <li>
          <button
            type="button"
            className="provit-page-btn provit-page-nav-btn"
            onClick={() => handlePageClick(startPage - 1)}
            disabled={!hasPrevBlock}
            aria-label="이전 페이지 목록으로 이동"
            title="이전 목록"
          >
            <ChevronLeft size={16} strokeWidth={2.2} />
          </button>
        </li>

        {/* 번호 버튼들 */}
        {pageNumbers.map((num) => {
          const isActive = num === currentPage;
          return (
            <li key={num}>
              <button
                type="button"
                className={`provit-page-btn ${isActive ? 'active' : ''}`}
                onClick={() => handlePageClick(num)}
                aria-current={isActive ? 'page' : undefined}
                aria-label={`페이지 ${num}`}
              >
                {num}
              </button>
            </li>
          );
        })}

        {/* 다음 페이지 블록으로 이동 (>) */}
        <li>
          <button
            type="button"
            className="provit-page-btn provit-page-nav-btn"
            onClick={() => handlePageClick(endPage + 1)}
            disabled={!hasNextBlock}
            aria-label="다음 페이지 목록으로 이동"
            title="다음 목록"
          >
            <ChevronRight size={16} strokeWidth={2.2} />
          </button>
        </li>

        {/* 맨 마지막 페이지로 이동 */}
        {showFirstLast && (
          <li>
            <button
              type="button"
              className="provit-page-btn provit-page-nav-btn"
              onClick={() => handlePageClick(totalPages)}
              disabled={!hasNext}
              aria-label="마지막 페이지로 이동"
              title="마지막 페이지"
            >
              <ChevronsRight size={16} strokeWidth={2.2} />
            </button>
          </li>
        )}
      </ul>
    </nav>
  );
}

export default Pagination;
