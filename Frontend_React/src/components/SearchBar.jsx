import { useEffect, useState } from 'react';
import './SearchBar.css';

function SearchBar({ value = '', placeholder = 'Tìm sản phẩm...', onSearch }) {
  const [keyword, setKeyword] = useState(value);

  useEffect(() => {
    setKeyword(value);
  }, [value]);

  function handleSubmit(event) {
    event.preventDefault();

    if (typeof onSearch === 'function') {
      onSearch(keyword.trim());
    }
  }

  return (
    <form className="shop-search" onSubmit={handleSubmit} role="search" aria-label="Tìm kiếm sản phẩm">
      <input
        type="text"
        className="shop-search__input"
        placeholder={placeholder}
        value={keyword}
        onChange={(event) => setKeyword(event.target.value)}
      />
      <button type="submit" className="shop-search__btn" aria-label="Tìm kiếm">
        <svg viewBox="0 0 24 24" aria-hidden="true" className="shop-search__icon">
          <path d="M10.5 3a7.5 7.5 0 0 1 5.94 12.08l4.24 4.24a1 1 0 1 1-1.42 1.42l-4.24-4.24A7.5 7.5 0 1 1 10.5 3Zm0 2a5.5 5.5 0 1 0 0 11 5.5 5.5 0 0 0 0-11Z" />
        </svg>
      </button>
    </form>
  );
}

export default SearchBar;
