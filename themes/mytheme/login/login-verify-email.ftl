<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
  <#if section = "title">
    Email Verification
  <#elseif section = "header">
    <h1 style="text-align:center; color:#4CAF50;">Email Verified</h1>
  <#elseif section = "form">
    <div style="text-align:center; padding:40px;">
        <p>Your email <b>${user.email!''}</b> has been successfully verified.</p>
        <p>
          You can now 
          <a href="${url.redirectUri!'http://localhost:3000/verify-email'}" 
             style="color:#4CAF50; font-weight:bold;">
             log in
          </a> 
          to your account.
        </p>
    </div>
  </#if>
</@layout.registrationLayout>
