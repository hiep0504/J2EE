import { Link } from 'react-router-dom';
import './Footer.css';

function Footer() {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="shop-footer mt-auto">
      <div className="shop-footer__inner">
        <div className="shop-footer__brand">
          <h3>ShopApp</h3>
          <p>Phong cách trẻ trung, giao hàng nhanh và trải nghiệm mua sắm mượt mà.</p>
        </div>

        <div className="shop-footer__menu">
          <Link to="/">Sản phẩm</Link>
          <Link to="/cart">Giỏ hàng</Link>
          <Link to="/order/history">Lịch sử đơn</Link>
          <Link to="/account">Tài khoản</Link>
        </div>

        <div className="shop-footer__contact">
          <span>Hotline: 0901 234 567</span>
          <span>Email: support@shopapp.vn</span>
          <span>Giờ làm việc: 8:00 - 22:00</span>
        </div>
      </div>

      <p className="shop-footer__copy">© {currentYear} ShopApp. All rights reserved.</p>
    </footer>
  );
}

export default Footer;
