<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Thông Tin Xác Thực Đã Được Cập Nhật</title>
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
                    
                    <!-- Content -->
                    <tr>
                        <td style="padding: 40px 30px;">
                            <h2 style="color: #333333; margin: 0 0 20px 0; font-size: 24px;">Thông Tin Xác Thực Đã Được Cập Nhật</h2>
                            
                            <p style="color: #555555; line-height: 1.6; margin: 0 0 15px 0;">
                                Xin chào <strong>${user.firstName!""} ${user.lastName!""}</strong>,
                            </p>
                            
                            <p style="color: #555555; line-height: 1.6; margin: 0 0 15px 0;">
                                Thông tin xác thực tài khoản của bạn đã được cập nhật thành công.
                            </p>
                            
                            <#if event?? && event.type??>
                            <div style="background-color: #d1ecf1; border-left: 4px solid #17a2b8; padding: 15px; margin: 20px 0; border-radius: 4px;">
                                <p style="color: #0c5460; margin: 0; font-size: 14px;">
                                    <strong>Loại thay đổi:</strong> ${event.type}
                                </p>
                            </div>
                            </#if>
                            
                            <div style="background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; border-radius: 4px;">
                                <p style="color: #856404; margin: 0; font-size: 14px;">
                                    ⚠️ <strong>Lưu ý:</strong> Nếu bạn không thực hiện thay đổi này, vui lòng liên hệ với quản trị viên ngay lập tức.
                                </p>
                            </div>
                            
                            <#if event?? && event.time??>
                            <p style="color: #777777; font-size: 13px; margin: 20px 0 0 0;">
                                Thời gian: ${event.time}
                            </p>
                            </#if>
                        </td>
                    </tr>
                    
                    <!-- Footer -->
                    <tr>
                        <td style="background-color: #f8f9fa; padding: 20px 30px; text-align: center; border-radius: 0 0 8px 8px;">
                            <p style="color: #999999; font-size: 12px; margin: 0;">
                                © 2025 NEXO NETWORK. All rights reserved.
                            </p>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>
</body>
</html>
