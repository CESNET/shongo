<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8" />
    <title></title>
    ${head}
</head>
<body>

<!-- Content wrapper: begin -->
<div id="wrapper">

    <!-- CESNET's linker: start -->
    <div id="cesnet_linker_placeholder"
         <#if !user??>data-login-href="/login"</#if>
         data-lang="en"
         data-lang-cs-href="?lang=cs"
         data-lang-en-href="?lang=en">
    </div>
    <!-- CESNET's linker: end -->

    <!-- Content placeholder: begin -->
    <div id="content_placeholder" class="container">
        ${content}
    </div>
    <!-- Content placeholder: end -->

    <div class="push"></div>
</div>
<!-- Content wrapper: end -->

<!-- CESNET's footer: start -->
<div id="footer">
    <footer>
        <div class="container">
            <div class="row">
                <div class="col col-md-3">
                    <div class="logo-wrapper">
                        <img src="${url.resources}/img/logo-cesnet.png" class="img-responsive" alt="cesnet logo">
                    </div>
                </div>
                <div class="col-lg-7 col-lg-push-2 col-md-push-1 col-md-8">
                    <div class="row">
                        <div class="col col-sm-4">
                            <h2>Rychlé odkazy</h2>
                            <ul>
                                <li><a href="#">CESNET PKI</a></li>
                                <li><a href="#">eduID.cz</a></li>
                                <li><a href="#">eduroam</a></li>
                                <li><a href="#">MetaCentrum</a></li>
                                <li><a href="#">PERUN</a></li>
                            </ul>
                        </div>
                        <div class="col col-sm-4">
                            <h2>Kontakt</h2>
                            CESNET, z. s. p. o<br/>
                            ZIKOVA 4, 16000 PRAHA <br/>
                            TEL : +420 224 352 994<br/>
                            FAX : +420 224 320 269<br/>
                            <a href="mailto:info@cesnet.cz">info@cesnet.cz</a>
                        </div>
                        <div class="col col-sm-4">
                            <h2>Stálá služba</h2>
                            TEL: +420 224 352 994<br/>
                            GSM: +420 602 252 531<br/>
                            FAX: +420 224 313 211<br/>
                            <a href="mailto:support@cesnet.cz">support@cesnet.cz</a>
                        </div>
                    </div>
                </div>
            </div>
            <div class="row">
                <div class="col col-sm-12 copyright">
                    © 1991–2014 CESNET, z. s. p. o
                </div>
            </div>
        </div>
    </footer>
</div>
<!-- CESNET's footer: end -->

<!-- CESNET's linker (JS): start -->
<!-- <script type="text/javascript" async src="https://linker.cesnet.cz/linker.js"></script>-->
<script type="text/javascript" async src="https://shongo.cesnet.cz/linker-fix/linker-fixed.js"></script>
<!-- CESNET's linker (JS): end -->

</body>
</html>