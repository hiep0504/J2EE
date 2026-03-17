import { Link } from 'react-router-dom';

function Header() {
  return (
    <nav className="navbar navbar-dark bg-dark px-4">
      <Link to="/" className="navbar-brand fw-bold">ShopApp</Link>
    </nav>
  );
}

export default Header;
