<html>
  <body style="font-family: Arial, sans-serif; background:#f9f9f9; padding:20px;">
    <table align="center" width="600" cellpadding="0" cellspacing="0" style="background:#ffffff; border-radius:8px; overflow:hidden; box-shadow:0 2px 8px rgba(0,0,0,0.1);">
      <tr>
        <td style="padding:20px; text-align:center; background:#4CAF50; color:white;">
          <h1>Nexo-network</h1>
        </td>
      </tr>
      <tr>
        <td style="padding:30px; color:#333;">
          <h2 style="margin-top:0;">Chào mừng bạn, </h2>
          <p>Bạn đã đăng ký với email: <b>${user.email}</b></p>
          <p>Vui lòng xác nhận địa chỉ email của bạn bằng cách nhấp vào nút bên dưới:</p>
          <p style="text-align:center; margin:30px 0;">
            <a href="${link}" style="display:inline-block; padding:12px 25px; background:#4CAF50; color:#fff; text-decoration:none; border-radius:5px;">
              Xác minh Email
            </a>
          </p>
          <p>Liên kết này có hiệu lực trong <b>${linkExpirationFormatter(linkExpiration)}</b>.</p>
          <p>Nếu bạn không tạo tài khoản này, bạn có thể bỏ qua email này.</p>
        </td>
      </tr>
      <tr>
        <td style="padding:15px; text-align:center; font-size:12px; color:#aaa;">
          © 2025 Nexo-network. Đã đăng ký bản quyền.
        </td>
      </tr>
    </table>
  </body>
</html>
