(function () {
    const LOGIN_PAGE = 'login.html';
    const WORK_PAGE = 'work.html';

    function hasBridgeMethod(name) {
        return typeof window.Bridge !== 'undefined' && window.Bridge && typeof window.Bridge[name] === 'function';
    }

    function safeParseJson(text, fallback) {
        if (!text) {
            return fallback;
        }
        try {
            return JSON.parse(text);
        } catch (error) {
            console.error('JSON parse failed:', error, text);
            return fallback;
        }
    }

    function redirect(page) {
        if (location.pathname.endsWith('/' + page) || location.href.endsWith('/' + page) || location.href.endsWith(page)) {
            return;
        }
        location.href = page;
    }

    function getAuthState() {
        if (hasBridgeMethod('getAuthState')) {
            return safeParseJson(window.Bridge.getAuthState(), { isLoggedIn: false, jwtValid: false });
        }
        return { isLoggedIn: false, jwtValid: false, browserMode: true };
    }

    function setStatus(element, text, type) {
        if (!element) {
            return;
        }
        element.textContent = text;
        element.className = 'status ' + (type || 'info');
    }

    function readFormDraft() {
        return {
            studentId: localStorage.getItem('cg.studentId') || '',
            studentName: localStorage.getItem('cg.studentName') || ''
        };
    }

    function saveFormDraft(studentId, studentName) {
        localStorage.setItem('cg.studentId', studentId || '');
        localStorage.setItem('cg.studentName', studentName || '');
    }

    function initIndexPage() {
        const status = document.getElementById('indexStatus');
        const auth = getAuthState();
        if (auth.jwtValid) {
            setStatus(status, '检测到有效登录，正在进入工作页...', 'success');
            redirect(WORK_PAGE);
        } else {
            setStatus(status, '未检测到有效登录，正在进入登录页...', 'info');
            redirect(LOGIN_PAGE);
        }
    }

    function initLoginPage() {
        const status = document.getElementById('loginStatus');
        const loginButton = document.getElementById('loginButton');
        const refreshStatusButton = document.getElementById('refreshStatusButton');

        function refresh() {
            const auth = getAuthState();
            if (auth.jwtValid) {
                setStatus(status, '检测到有效 JWT，正在跳转到工作页...', 'success');
                redirect(WORK_PAGE);
                return;
            }

            if (auth.browserMode) {
                setStatus(status, '当前处于浏览器预览模式，无法调用原生登录。请在 App WebView 中打开。', 'error');
                return;
            }

            setStatus(status, '当前未登录或 JWT 已过期，请先完成统一认证。', 'info');
        }

        if (loginButton) {
            loginButton.addEventListener('click', function () {
                if (!hasBridgeMethod('startOAuthPage')) {
                    setStatus(status, '当前环境不支持原生桥接，无法打开统一认证。', 'error');
                    return;
                }
                setStatus(status, '正在打开统一认证页面...', 'info');
                window.Bridge.startOAuthPage();
            });
        }

        if (refreshStatusButton) {
            refreshStatusButton.addEventListener('click', refresh);
        }

        refresh();
    }

    function initWorkTabs() {
        const tabButtons = Array.prototype.slice.call(document.querySelectorAll('.tab-button'));
        const tabPanels = Array.prototype.slice.call(document.querySelectorAll('.tab-panel'));

        if (!tabButtons.length || !tabPanels.length) {
            return;
        }

        function setActiveTab(targetId) {
            tabButtons.forEach(function (button) {
                const active = button.dataset.tabTarget === targetId;
                button.classList.toggle('is-active', active);
                button.setAttribute('aria-selected', active ? 'true' : 'false');
            });

            tabPanels.forEach(function (panel) {
                const active = panel.id === targetId;
                panel.classList.toggle('is-active', active);
                panel.hidden = !active;
            });
        }

        tabButtons.forEach(function (button) {
            button.addEventListener('click', function () {
                const targetId = button.dataset.tabTarget;
                if (!targetId) {
                    return;
                }
                setActiveTab(targetId);
            });
        });

        setActiveTab('workPanel');
    }

    function initWorkPage() {
        const auth = getAuthState();
        const status = document.getElementById('workStatus');
        const studentIdInput = document.getElementById('studentId');
        const studentNameInput = document.getElementById('studentName');
        const generateButton = document.getElementById('generateButton');
        const clearOutputButton = document.getElementById('clearOutputButton');
        const logoutButton = document.getElementById('logoutButton');
        const output = document.getElementById('output');

        if (!auth.jwtValid) {
            setStatus(status, '登录已失效，正在返回登录页...', 'error');
            redirect(LOGIN_PAGE);
            return;
        }

        initWorkTabs();

        const draft = readFormDraft();
        if (studentIdInput) {
            studentIdInput.value = draft.studentId;
        }
        if (studentNameInput) {
            studentNameInput.value = draft.studentName;
        }

        if (generateButton) {
            generateButton.addEventListener('click', function () {
                const studentId = (studentIdInput && studentIdInput.value || '').trim();
                const studentName = (studentNameInput && studentNameInput.value || '').trim();

                if (!studentId) {
                    setStatus(status, '请输入学号。', 'error');
                    studentIdInput && studentIdInput.focus();
                    return;
                }
                if (!studentName) {
                    setStatus(status, '请输入姓名。', 'error');
                    studentNameInput && studentNameInput.focus();
                    return;
                }
                if (!hasBridgeMethod('buildUploadJsonSports')) {
                    setStatus(status, '当前环境不支持原生生成接口。', 'error');
                    return;
                }

                saveFormDraft(studentId, studentName);
                setStatus(status, '正在生成 UploadJsonSports...', 'info');
                const resultText = window.Bridge.buildUploadJsonSports(studentId, studentName);
                const result = safeParseJson(resultText, null);

                if (!result) {
                    setStatus(status, '生成失败：返回内容不是有效 JSON。', 'error');
                    output.value = resultText || '';
                    return;
                }
                if (result.error) {
                    setStatus(status, result.error, 'error');
                    output.value = JSON.stringify(result, null, 2);
                    return;
                }

                setStatus(status, '生成成功，可编辑 JSON 后发送。', 'success');
                output.value = JSON.stringify(result, null, 2);
            });
        }

        const formatButton = document.getElementById('formatButton');
        if (formatButton) {
            formatButton.addEventListener('click', function () {
                const currentText = output.value.trim();
                if (!currentText) {
                    setStatus(status, '没有内容可格式化。', 'error');
                    return;
                }

                const parsed = safeParseJson(currentText, null);
                if (!parsed) {
                    setStatus(status, '格式化失败：JSON 格式不合法。', 'error');
                    return;
                }

                output.value = JSON.stringify(parsed, null, 2);
                setStatus(status, '格式化成功。', 'success');
            });
        }

        const copyButton = document.getElementById('copyButton');
        if (copyButton) {
            copyButton.addEventListener('click', function () {
                const text = output.value.trim();
                if (!text) {
                    setStatus(status, '没有内容可复制。', 'error');
                    return;
                }

                if (navigator.clipboard && navigator.clipboard.writeText) {
                    navigator.clipboard.writeText(text).then(function () {
                        setStatus(status, '已复制到剪贴板。', 'success');
                    }).catch(function () {
                        fallbackCopy(text);
                    });
                } else {
                    fallbackCopy(text);
                }

                function fallbackCopy(text) {
                    const textarea = document.createElement('textarea');
                    textarea.value = text;
                    document.body.appendChild(textarea);
                    textarea.select();
                    try {
                        document.execCommand('copy');
                        setStatus(status, '已复制到剪贴板。', 'success');
                    } catch (err) {
                        setStatus(status, '复制失败。', 'error');
                    }
                    document.body.removeChild(textarea);
                }
            });
        }

        const submitButton = document.getElementById('submitButton');
        if (submitButton) {
            submitButton.addEventListener('click', function () {
                const currentOutput = output.value.trim();
                if (!currentOutput || currentOutput === '{}') {
                    setStatus(status, '请先生成 UploadJsonSports。', 'error');
                    return;
                }

                const resultJson = safeParseJson(currentOutput, null);
                if (!resultJson || resultJson.error) {
                    setStatus(status, '当前输出不是有效的 UploadJsonSports JSON。', 'error');
                    return;
                }

                if (!hasBridgeMethod('submitSportsData')) {
                    setStatus(status, '当前环境不支持 HTTP 提交接口。', 'error');
                    return;
                }

                setStatus(status, '正在发送 HTTP 请求...', 'info');
                const submitResult = window.Bridge.submitSportsData(currentOutput);
                const submitResponse = safeParseJson(submitResult, null);

                if (!submitResponse) {
                    setStatus(status, '服务器响应不是有效 JSON。', 'error');
                    output.value = submitResult || '';
                    return;
                }

                if (submitResponse.error) {
                    setStatus(status, '提交失败：' + submitResponse.error, 'error');
                    output.value = JSON.stringify(submitResponse, null, 2);
                    return;
                }

                setStatus(status, 'HTTP 请求成功！', 'success');
                output.value = JSON.stringify(submitResponse, null, 2);
            });
        }

        if (clearOutputButton) {
            clearOutputButton.addEventListener('click', function () {
                output.value = '{}';
                setStatus(status, '结果已清空。', 'info');
            });
        }

        if (logoutButton) {
            logoutButton.addEventListener('click', function () {
                if (hasBridgeMethod('logout')) {
                    window.Bridge.logout();
                    return;
                }
                redirect(LOGIN_PAGE);
            });
        }

        setStatus(status, '已登录，可以开始生成 UploadJsonSports。', 'success');

        // 初始化高级设置面板
        initAdvancedPanel();
    }

    function initAdvancedPanel() {
        const advancedStatus = document.getElementById('advancedStatus');
        const credentialsOutput = document.getElementById('credentialsOutput');
        const refreshCredsButton = document.getElementById('refreshCredsButton');
        const copyCredsButton = document.getElementById('copyCresButton');
        const pasteCredsButton = document.getElementById('pasteCredsButton');
        const clearCredsButton = document.getElementById('clearCredsButton');

        function setAdvancedStatus(text, type) {
            if (!advancedStatus) {
                return;
            }
            advancedStatus.textContent = text;
            advancedStatus.className = 'status ' + (type || 'info');
        }

        function loadCredentials() {
            const authState = getAuthState();

            if (authState.browserMode) {
                credentialsOutput.value = JSON.stringify({
                    jwt: '',
                    secret: ''
                }, null, 2);
                setAdvancedStatus('当前处于浏览器预览模式，请在 App WebView 中查看当前凭据。', 'info');
                return;
            }

            if (!authState.jwtValid) {
                credentialsOutput.value = JSON.stringify({
                    jwt: authState.jwt || '',
                    secret: authState.secret || ''
                }, null, 2);
                setAdvancedStatus('登录已失效，请重新登录后再查看或切换凭据。', 'error');
                return;
            }

            if (!authState.jwt || !authState.secret) {
                credentialsOutput.value = JSON.stringify({
                    jwt: authState.jwt || '',
                    secret: authState.secret || ''
                }, null, 2);
                setAdvancedStatus('当前登录态有效，但凭据字段缺失，已尝试读取本地存储。请重新登录一次以刷新凭据。', 'error');
                return;
            }

            credentialsOutput.value = JSON.stringify({
                jwt: authState.jwt,
                secret: authState.secret
            }, null, 2);
            setAdvancedStatus('已读取当前登录凭据。', 'success');
        }

        if (refreshCredsButton) {
            refreshCredsButton.addEventListener('click', loadCredentials);
        }

        if (copyCredsButton) {
            copyCredsButton.addEventListener('click', function () {
                const text = credentialsOutput.value.trim();
                if (!text) {
                    setAdvancedStatus('没有内容可复制。', 'error');
                    return;
                }

                if (navigator.clipboard && navigator.clipboard.writeText) {
                    navigator.clipboard.writeText(text).then(function () {
                        setAdvancedStatus('已复制凭据到剪贴板。', 'success');
                    }).catch(function () {
                        fallbackCopy(text);
                    });
                } else {
                    fallbackCopy(text);
                }

                function fallbackCopy(text) {
                    const textarea = document.createElement('textarea');
                    textarea.value = text;
                    document.body.appendChild(textarea);
                    textarea.select();
                    try {
                        document.execCommand('copy');
                        setAdvancedStatus('已复制凭据到剪贴板。', 'success');
                    } catch (err) {
                        setAdvancedStatus('复制失败。', 'error');
                    }
                    document.body.removeChild(textarea);
                }
            });
        }

        if (pasteCredsButton) {
            pasteCredsButton.addEventListener('click', function () {
                const currentText = credentialsOutput.value.trim();
                if (!currentText) {
                    setAdvancedStatus('请粘贴有效的 JSON 凭据。', 'error');
                    return;
                }

                const parsed = safeParseJson(currentText, null);
                if (!parsed || typeof parsed !== 'object') {
                    setAdvancedStatus('JSON 格式不合法。', 'error');
                    return;
                }

                if (!parsed.jwt || !parsed.secret) {
                    setAdvancedStatus('凭据必须包含 jwt 和 secret 字段。', 'error');
                    return;
                }

                if (!hasBridgeMethod('applyCredentials')) {
                    setAdvancedStatus('当前环境不支持应用凭据。', 'error');
                    return;
                }

                setAdvancedStatus('正在应用凭据...', 'info');
                const result = window.Bridge.applyCredentials(JSON.stringify(parsed));
                const resultObj = safeParseJson(result, null);

                if (!resultObj) {
                    setAdvancedStatus('应用凭据失败：返回数据不合法。', 'error');
                    return;
                }

                if (resultObj.error) {
                    setAdvancedStatus('应用凭据失败：' + resultObj.error, 'error');
                    return;
                }

                loadCredentials();
                setAdvancedStatus('凭据已成功应用，当前用户已切换。', 'success');
            });
        }

        if (clearCredsButton) {
            clearCredsButton.addEventListener('click', function () {
                credentialsOutput.value = '{}';
                setAdvancedStatus('凭据已清空。', 'info');
            });
        }

        // 初始加载凭据
        loadCredentials();
    }

    document.addEventListener('DOMContentLoaded', function () {
        const page = document.body && document.body.dataset ? document.body.dataset.page : '';
        if (page === 'index') {
            initIndexPage();
            return;
        }
        if (page === 'login') {
            initLoginPage();
            return;
        }
        if (page === 'work') {
            initWorkPage();
        }
    });
})();
