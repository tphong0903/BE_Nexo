<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
  <#if section = "title">
    Xác Minh Email
  <#elseif section = "header">
    <h1 style="text-align:center; color:#007bff;">Xác Minh Email</h1>
  <#elseif section = "form">
    <div style="text-align:center; padding:40px;">
        <p style="color:#555; line-height:1.6;">
          Tài khoản của bạn <b>${user.email!''}</b> chưa được xác minh.
        </p>
        <p style="color:#555; line-height:1.6;">
          Chúng tôi đã gửi một email kèm liên kết xác minh tới hộp thư của bạn.
          Vui lòng kiểm tra email (kể cả mục Spam) và bấm vào liên kết để hoàn tất xác minh.
        </p>
        <p style="color:#555; line-height:1.6; margin-top:20px;">
          Chưa nhận được email?
          <a href="${url.loginAction}" style="color:#007bff; font-weight:bold;">
             Gửi lại email xác minh
          </a>
        </p>
    </div>
  </#if>
</@layout.registrationLayout>
