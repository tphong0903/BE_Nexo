<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <style>
        body {
            font-family: 'Inter', 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
            background-color: #f4f7f6;
            margin: 0;
            padding: 0;
            -webkit-font-smoothing: antialiased;
        }
        .container {
            max-width: 600px;
            margin: 40px auto;
            background-color: #ffffff;
            border-radius: 12px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.05);
            overflow: hidden;
            border: 1px solid #e1e4e8;
        }
        .header {
            background-color: #d93025; /* Màu đỏ cảnh báo */
            padding: 24px;
            text-align: center;
        }
        .header h1 {
            color: #ffffff;
            margin: 0;
            font-size: 24px;
            font-weight: 600;
        }
        .content {
            padding: 32px 24px;
            color: #3c4043;
            line-height: 1.6;
            font-size: 16px;
        }
        .info-box {
            background-color: #f8f9fa;
            border-left: 4px solid #d93025;
            padding: 16px;
            margin: 24px 0;
            border-radius: 0 8px 8px 0;
        }
        .info-item {
            margin-bottom: 8px;
        }
        .info-item:last-child {
            margin-bottom: 0;
        }
        .info-label {
            font-weight: 600;
            color: #5f6368;
            display: inline-block;
            width: 100px;
        }
        .footer {
            background-color: #f8f9fa;
            padding: 16px 24px;
            text-align: center;
            color: #80868b;
            font-size: 13px;
            border-top: 1px solid #e1e4e8;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>Cảnh báo bảo mật</h1>
        </div>
        <div class="content">
            <p>Chào bạn,</p>
            <p>Hệ thống của chúng tôi vừa ghi nhận một <strong>lần đăng nhập thất bại</strong> vào tài khoản của bạn. Chi tiết như sau:</p>
            
            <div class="info-box">
                <div class="info-item">
                    <span class="info-label">Thời gian:</span>
                    <span>${event.date?datetime?string('dd/MM/yyyy HH:mm:ss')}</span>
                </div>
                <div class="info-item">
                    <span class="info-label">Địa chỉ IP:</span>
                    <span>${event.ipAddress}</span>
                </div>
            </div>

            <p>Nếu bạn vừa thử đăng nhập nhưng quên mật khẩu, bạn có thể thiết lập lại mật khẩu mới ở trang đăng nhập. Còn nếu <strong>không phải là bạn</strong>, có thể ai đó đang cố gắng truy cập trái phép. Vui lòng đổi mật khẩu ngay lập tức để bảo vệ tài khoản.</p>
        </div>
        <div class="footer">
            <p>Email này được gửi tự động từ hệ thống bảo mật. Vui lòng không trả lời email này.</p>
        </div>
    </div>
</body>
</html>
