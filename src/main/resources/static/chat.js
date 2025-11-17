// ===== HTML 요소 가져오기 =====
const musicScreen = document.getElementById("music-screen");
const chatScreen = document.getElementById("chat-screen");

const messagesEl = document.getElementById("messages");
const messageInput = document.getElementById("messageInput");
const sendBtn = document.getElementById("sendBtn");

const bgAudio = document.getElementById("bgAudio");
const bgImg = document.getElementById("background-img");

// ===== 전역 사운드 매핑 =====
const soundMap = {
    breeze: "/sounds/breeze.mp3",
    forest: "/sounds/forest.mp3",
    ocean: "/sounds/ocean.mp3"
};

let chatStarted = false;
const sessionId = "test_user_001"; // 실제 앱에서는 고유 세션 사용

// ===== 음악 재생 =====
function playAudio(key) {
    if (!key || key === "none" || !soundMap[key]) {
        bgAudio.pause();
        bgAudio.src = "";
        return;
    }

    bgAudio.src = soundMap[key];
    bgAudio.volume = 0.6;
    bgAudio.play().catch(() => {});
}

// ===== 배경 이미지 변경 =====
function updateBackground(soundKey) {
    const bgMap = {
        breeze: "/images/breeze.png",
        forest: "/images/forest.png",
        ocean: "/images/ocean.png",
        none: "/images/none.png"
    };

    bgImg.src = bgMap[soundKey] || bgMap.none;
}

// ===== 채팅 시작 =====
function startChat() {
    if (!chatStarted) {
        addBotMessage("오늘 하루는 어땠어?");
        chatStarted = true;
    }
    musicScreen.classList.add("hidden");
    chatScreen.classList.remove("hidden");
}

// ===== 메시지 DOM 추가 =====
function addBotMessage(text) {
    const div = document.createElement("div");
    div.className = "bubble bot";
    div.innerText = text;
    messagesEl.appendChild(div);
    messagesEl.scrollTop = messagesEl.scrollHeight;
}

function addUserMessage(text) {
    const div = document.createElement("div");
    div.className = "bubble user";
    div.innerText = text;
    messagesEl.appendChild(div);
    messagesEl.scrollTop = messagesEl.scrollHeight;
}

// ===== 음악 선택 버튼 이벤트 =====
document.querySelectorAll(".music-btn").forEach(btn => {
    btn.addEventListener("click", () => {
        const sound = btn.dataset.sound;

        // 배경 변경
        updateBackground(sound);

        // 음악 재생
        playAudio(sound);

        // 채팅 화면 전환
        startChat();

        // 선택 저장
        localStorage.setItem("selectedSound", sound);
    });
});

// ===== 채팅 전송 =====
sendBtn.onclick = async () => {
    const text = messageInput.value.trim();
    if (!text) return;

    addUserMessage(text);
    messageInput.value = "";

    const selectedSound = localStorage.getItem("selectedSound") || "none";

    try {
        const res = await fetch("http://localhost:8080/api/chat/process", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                sessionId: sessionId,
                userMessage: text,
                soundId: selectedSound
            })
        });

        if (res.ok) {
            const data = await res.json();

            addBotMessage(data.aiResponse);

            // 배경 이미지 업데이트
            if (data.backgroundImageUrl) {
                bgImg.src = "/" + data.backgroundImageUrl;
            }

            // 서버에서 반환된 사운드 적용
            if (data.currentSoundId) {
                localStorage.setItem("selectedSound", data.currentSoundId);
                playAudio(data.currentSoundId);
            }

        } else {
            addBotMessage("서버와 연결할 수 없어 ㅠㅠ");
        }
    } catch (err) {
        console.error(err);
        addBotMessage("서버와 연결할 수 없어 ㅠㅠ");
    }
};

// ===== 초기화 =====
function init() {
    // 항상 초기 배경 none.png
    bgImg.src = "/images/none.png";

    const savedSound = localStorage.getItem("selectedSound");

    if (savedSound) {
        // 이전에 선택한 사운드만 배경 적용, 음악 선택 화면 유지
        updateBackground(savedSound);
        musicScreen.classList.remove("hidden");
        chatScreen.classList.add("hidden");
        chatStarted = false;
    } else {
        musicScreen.classList.remove("hidden");
        chatScreen.classList.add("hidden");
    }
}

init();
