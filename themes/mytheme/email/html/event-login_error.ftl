<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Cảnh Báo Đăng Nhập Thất Bại</title>
</head>
<body style="margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;">
    <table width="100%" cellpadding="0" cellspacing="0" style="background-color: #f4f4f4; padding: 20px;">
        <tr>
            <td align="center">
                <table width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">

                    <!-- Header -->
                    <tr>
                        <td style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 30px; text-align: center; border-radius: 8px 8px 0 0;">
                            <h1 style="color: #ffffff; margin: 0; font-size: 28px; font-weight: 700;">NEXO NETWORK</h1>
                        </td>
                    </tr>

                    <!-- Alert Banner -->
                    <tr>
                        <td style="background-color: #dc3545; padding: 15px 30px; text-align: center;">
                            <p style="color: #ffffff; margin: 0; font-size: 15px; font-weight: 600;">
                                🔒 Cảnh Báo Bảo Mật Tài Khoản
                            </p>
                        </td>
                    </tr>

                    <!-- Content -->
                    <tr>
                        <td style="padding: 40px 30px;">
                            <h2 style="color: #333333; margin: 0 0 20px 0; font-size: 22px;">Phát Hiện Đăng Nhập Thất Bại</h2>

                            <p style="color: #555555; line-height: 1.6; margin: 0 0 15px 0;">
                                Xin chào <strong>${user.firstName!""} ${user.lastName!""}</strong>,
                            </p>

                            <p style="color: #555555; line-height: 1.6; margin: 0 0 25px 0;">
                                Chúng tôi phát hiện có một lần đăng nhập <strong>thất bại</strong> vào tài khoản của bạn.
                                Nếu đây không phải bạn, hãy đổi mật khẩu ngay lập tức để bảo vệ tài khoản.
                            </p>

                            <!-- Info Box -->
                            <table width="100%" cellpadding="0" cellspacing="0" style="margin-bottom: 20px;">
                                <tr>
                                    <td style="background-color: #f8f9fa; border-radius: 6px; padding: 20px; border: 1px solid #e9ecef;">
                                        <table width="100%" cellpadding="0" cellspacing="0">
                                            <tr>
                                                <td style="padding: 6px 0; border-bottom: 1px solid #dee2e6;">
                                                    <span style="color: #888888; font-size: 13px;">📅 Thời gian</span>
                                                </td>
                                                <td style="padding: 6px 0; border-bottom: 1px solid #dee2e6; text-align: right;">
                                                    <strong style="color: #333333; font-size: 13px;">${event.date}</strong>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td style="padding: 6px 0; padding-top: 12px;">
                                                    <span style="color: #888888; font-size: 13px;">🌐 Địa chỉ IP</span>
                                                </td>
                                                <td style="padding: 6px 0; padding-top: 12px; text-align: right;">
                                                    <strong style="color: #333333; font-size: 13px;">${event.ipAddress}</strong>
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>
                            </table>

                            <!-- Warning Box -->
                            <div style="background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px 20px; margin: 20px 0; border-radius: 4px;">
                                <p style="color: #856404; margin: 0; font-size: 14px; line-height: 1.6;">
                                    ⚠️ <strong>Lưu ý:</strong> Nếu bạn không thực hiện đăng nhập này, tài khoản của bạn có thể đang bị xâm phạm.
                                    Vui lòng thay đổi mật khẩu ngay và liên hệ đội hỗ trợ nếu cần thiết.
                                </p>
                            </div>

                            <!-- Danger Box -->
                            <div style="background-color: #f8d7da; border-left: 4px solid #dc3545; padding: 15px 20px; margin: 20px 0; border-radius: 4px;">
                                <p style="color: #721c24; margin: 0; font-size: 14px; line-height: 1.6;">
                                    🚨 <strong>Khuyến nghị:</strong> Nếu bạn nhận được nhiều email thông báo như thế này liên tiếp,
                                    hãy bật xác thực hai yếu tố (2FA) để tăng cường bảo mật tài khoản.
                                </p>
                            </div>

                        </td>
                    </tr>

                    <!-- Footer -->
                    <tr>
                        <td style="background-color: #f8f9fa; padding: 20px 30px; text-align: center; border-radius: 0 0 8px 8px;">
                            <p style="color: #999999; font-size: 12px; margin: 0;">
                                © 2025 NEXO NETWORK. All rights reserved.
                            </p>
                            <p style="color: #bbbbbb; font-size: 11px; margin: 5px 0 0 0;">
                                Đây là email tự động, vui lòng không reply lại email này.
                            </p>
                        </td>
                    </tr>

                </table>
            </td>
        </tr>
    </table>
</body>
</html>
