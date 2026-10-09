from __future__ import annotations
import json
import random
import time
import urllib.parse
import requests
import hmac
import base64
from hashlib import sha1
from typing import Tuple
import re
import os
import sys
import threading

from Crypto.PublicKey import RSA
from Crypto.Cipher import PKCS1_v1_5
import binascii

import customtkinter as ctk
import tkinter as tk
from tkinter import messagebox

T = {
    "es": {
        "app_title": "Webtoon Downloader - by EryxZar",
        "lang_sel": "Selecciona tu idioma / Select your language",
        "not_logged": "🔴 No Logueado",
        "login_btn": "Iniciar Sesión",
        "change_acc": "Cambiar Cuenta",
        "url_ph": "Ingresa URL o ID de la serie (ej: 2154)",
        "analyze": "Analizar",
        "ep_list": "Lista de Capítulos",
        "quick_sel": "Selección Rápida\n(Ej: 1-5, 8, 10)",
        "ranges": "Rangos...",
        "select": "Seleccionar",
        "clear": "Limpiar Selección",
        "download": "📥 DESCARGAR",
        "email": "Correo Electrónico:",
        "pass": "Contraseña:",
        "enter": "Ingresar",
        "err_empty": "Completa todos los campos.",
        "err_invalid": "Credenciales inválidas o error de API.",
        "err_login": "Excepción al iniciar sesión.",
        "err_no_login": "Debes iniciar sesión primero.",
        "err_url": "URL o ID inválido.",
        "err_fmt": "Formato de rango inválido. Usa comas y guiones (ej: 1, 3, 5-10)",
        "err_no_sel": "No has seleccionado ningún capítulo para descargar.",
        "val_session": "Validando sesión guardada...",
        "cookie_exp": "Cookies expiradas. Re-iniciando sesión con credenciales guardadas...",
        "conn_as": "Conectado como: {}",
        "err_auto": "Error al auto-loguear: {}",
        "init_login": "Iniciando sesión, por favor espera...",
        "analyzing": "\nAnalizando serie...",
        "no_series": "No se encontró la serie.",
        "series_info": "Serie: {} | {} Capítulos reportados.",
        "detect_fast": "Detectados {} capítulos Fast Pass/Premium ocultos. Verificando opciones y precios...",
        "analysis_ok": "✅ Análisis y recolección completada con éxito.",
        "sel_range": "Seleccionados por rango: {}",
        "init_dl": "\n🚀 INICIANDO DESCARGA...",
        "dl_done": "\n✅ PROCESO DE DESCARGA FINALIZADO.",
        "refreshing_list": "\n🔄 Actualizando estado de los capítulos automáticamente...",
        "eval_cap": "\n--- Evaluando Capítulo {} ---",
        "free_pub": "🔓 El capítulo es gratuito de acceso público.",
        "free_acq": "🔓 Ya tienes derecho adquirido sobre este capítulo.",
        "cap_bought_inf": "🔓 Capítulo comprado permanentemente (Infinito).",
        "cap_unl_temp": "🔓 Tienes acceso temporal a este capítulo.",
        "chk_opts": "🔒 Capítulo no liberado. Consultando opciones de descarga...",
        "ads_ok": "📺 Permite Ads. Simulando visualización (32s)...",
        "ad_claimed": "✅ Anuncio reclamado.",
        "pass_claimed": "✅ Pase diario reclamado.",
        "bought": "✅ Capítulo comprado con monedas.",
        "no_coins": "❌ Monedas insuficientes para comprar (tienes {}, necesitas {}).",
        "prem_closed": "❌ Capítulo premium cerrado (sin opciones libres). Saltando...",
        "err_imgs": "⚠️ Error al obtener las imágenes del episodio {}",
        "skip_exist": "Saltando, carpeta '{}/{}' ya existe en Download.",
        "saving_in": "📥 Guardando en: Download/{}/{}...",
        "dl_prog": "   -> Descargando: {}/{} imágenes...",
        "cap_done": "✅ Descarga completada.",
        "status_free": "[GRATIS]",
        "status_bought": "[COMPRADO]",
        "status_temp": "[DESBLOQUEADO (Temporal)]",
        "status_unl": "[DESBLOQUEADO]",
        "status_ads": "[PAGA: {}c / ADS DISPONIBLE]",
        "status_ads_only": "[SOLO ADS / PASS]",
        "status_paid": "[SOLO PAGA: {}c]",
        "fatal_err": "Error fatal en Cap {}: {}",
        "coins": "Coin: {}",
    },
    "en": {
        "app_title": "Webtoon Downloader - by EryxZar",
        "lang_sel": "Select your language / Selecciona tu idioma",
        "not_logged": "🔴 Not Logged In",
        "login_btn": "Login",
        "change_acc": "Change Account",
        "url_ph": "Enter URL or series ID (e.g., 2154)",
        "analyze": "Analyze",
        "ep_list": "Episode List",
        "quick_sel": "Quick Select\n(E.g., 1-5, 8, 10)",
        "ranges": "Ranges...",
        "select": "Select",
        "clear": "Clear Selection",
        "download": "📥 DOWNLOAD",
        "email": "Email Address:",
        "pass": "Password:",
        "enter": "Enter",
        "err_empty": "Please fill in all fields.",
        "err_invalid": "Invalid credentials or API error.",
        "err_login": "Exception during login.",
        "err_no_login": "You must log in first.",
        "err_url": "Invalid URL or ID.",
        "err_fmt": "Invalid range format. Use commas and hyphens (e.g., 1, 3, 5-10)",
        "err_no_sel": "You haven't selected any episodes to download.",
        "val_session": "Validating saved session...",
        "cookie_exp": "Cookies expired. Re-logging in with saved credentials...",
        "conn_as": "Logged in as: {}",
        "err_auto": "Error during auto-login: {}",
        "init_login": "Logging in, please wait...",
        "analyzing": "\nAnalyzing series...",
        "no_series": "Series not found.",
        "series_info": "Series: {} | {} Episodes reported.",
        "detect_fast": "Detected {} hidden Fast Pass/Premium episodes. Checking options and prices...",
        "analysis_ok": "✅ Analysis and data collection completed successfully.",
        "sel_range": "Selected by range: {}",
        "init_dl": "\n🚀 STARTING DOWNLOAD...",
        "dl_done": "\n✅ DOWNLOAD PROCESS FINISHED.",
        "refreshing_list": "\n🔄 Automatically updating episode statuses...",
        "eval_cap": "\n--- Evaluating Episode {} ---",
        "free_pub": "🔓 The episode is free for public access.",
        "free_acq": "🔓 You already own the rights to this episode.",
        "cap_bought_inf": "🔓 Episode purchased permanently (Infinite).",
        "cap_unl_temp": "🔓 You have temporary access to this episode.",
        "chk_opts": "🔒 Episode not free. Checking download options...",
        "ads_ok": "📺 Allows Ads. Simulating viewing (32s)...",
        "ad_claimed": "✅ Ad claimed.",
        "pass_claimed": "✅ Daily pass claimed.",
        "bought": "✅ Episode bought with coins.",
        "no_coins": "❌ Not enough coins to buy (have {}, need {}).",
        "prem_closed": "❌ Premium episode locked (no free options). Skipping...",
        "err_imgs": "⚠️ Error fetching images for episode {}",
        "skip_exist": "Skipping, folder '{}/{}' already exists in Download.",
        "saving_in": "📥 Saving in: Download/{}/{}...",
        "dl_prog": "   -> Downloading: {}/{} images...",
        "cap_done": "✅ Download completed.",
        "status_free": "[FREE]",
        "status_bought": "[PURCHASED]",
        "status_temp": "[UNLOCKED (Temp)]",
        "status_unl": "[UNLOCKED]",
        "status_ads": "[PAID: {}c / ADS AVAILABLE]",
        "status_ads_only": "[ONLY ADS / PASS]",
        "status_paid": "[ONLY PAID: {}c]",
        "fatal_err": "Fatal error in Ep {}: {}",
        "coins": "Coin: {}",
    }
}

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DOWNLOAD_DIR = os.path.join(BASE_DIR, "Download")
CONFIG_FILE = os.path.join(BASE_DIR, "config.json")

ctk.set_appearance_mode("Dark")
ctk.set_default_color_theme("blue")

def get_len_char(string):
    length = len(string)
    return chr(length)

def encrypt(session_key, email, password, n_value, e_value):
    message = get_len_char(session_key) + session_key + get_len_char(email) + email + get_len_char(password) + password
    evalue = int(n_value, 16)
    n = int(e_value, 16)
    public_key = RSA.construct((n, evalue))
    cipher = PKCS1_v1_5.new(public_key)
    message_bytes = message.encode('utf-8')
    encrypted_message = cipher.encrypt(message_bytes)
    return binascii.hexlify(encrypted_message).decode('utf-8')

class AuthError(Exception): pass
class TimeLimitError(Exception): pass

device_key_chars = "abcdefghijklmnopqrstuvwxyz0123456789"
api_key = b"gUtPzJFZch4ZyAGviiyH94P99lQ3pFdRTwpJWDlSGFfwgpr6ses5ALOxWHOIT7R1"
user_agent = "nApps (Android 14; SM-A236B; linewebtoon; 3.3.2)"
push_token = ""
session = requests.Session()

class WebtoonApiCall:
    def __init__(self, api: WebtoonApi, name: str) -> None:
        self.api = api
        self.name = name

    def __call__(self, method: str = "GET", return_cookies: bool = False, **kwargs) -> dict:
        default_params = {
            "v": kwargs.get("v", 1),
            "serviceZone": "GLOBAL",
            "language": "en",
            "locale": "en",
            "platform": "APP_ANDROID"
        }
        kwargs.update(default_params)
        url = f"https://global.apis.naver.com/lineWebtoon/webtoon/{self.name}"
        request = requests.models.PreparedRequest()
        request.prepare(url=url, params=kwargs)

        if request.url is None:
            raise ValueError("Error in parameters")

        return self.api.send_request(request.url, method, kwargs, return_cookies)

class WebtoonApi:
    def __init__(self):
        self.device_key = "".join(random.choice(device_key_chars) for _ in range(32))
        self.neo_ses = ""
        self.neo_chk = ""
        
    def load_session(self, neo_ses, neo_chk, device_key):
        self.neo_ses = neo_ses
        self.neo_chk = neo_chk
        self.device_key = device_key
        
    def login(self, username: str, password: str) -> None:
        self.rsa_keys = self.getRsaKey() 
        n_value = self.rsa_keys['nvalue']
        e_value = self.rsa_keys['evalue']
        session_key = self.rsa_keys['sessionKey']
        key_name = self.rsa_keys["keyName"]

        encrypted_password = encrypt(session_key, username, password, n_value, e_value)
        
        response = self.loginById(
            method="POST",
            serviceZone="GLOBAL",
            encpw=encrypted_password,
            loginType='EMAIL',
            v=3,
            language='en',
            encnm=key_name
        )
        self.neo_ses = response["ses"]

        response = self.setDeviceInfo(
            return_cookies=True,
            deviceKey=self.device_key,
            appType="LINEWEBTOON",
            pushToken=push_token,
            pushCode="FCMV1",
            serviceZone="GLOBAL",
            v=1,
            language="en",
            locale="en",
            platform="APP_ANDROID"
        )
        self.neo_chk = self.extract_cookie(response["headers"], "NEO_CHK")

    def __generate_signature(self, data: bytes) -> str:
        mac = hmac.new(api_key, data, sha1)
        return base64.b64encode(mac.digest()).decode('utf-8').strip()

    def get_current_time(self) -> str:
        url = "https://global.apis.naver.com/currentTime"
        response = session.get(url, headers={"User-Agent": user_agent})
        response.raise_for_status()  
        return response.content

    def get_signed_url(self, webtoon_url: str) -> Tuple[str, str, str]:
        url = webtoon_url.encode("utf-8")
        current_time = self.get_current_time()
        signature = self.__generate_signature(url[:255] + current_time)
        return webtoon_url, current_time.decode(), signature

    def send_request(self, unsigned_url: str, method: str, data: dict = None, return_cookies: bool = False) -> dict:
        webtoon_url, msgpad, md = self.get_signed_url(unsigned_url)
        headers = {
            "User-Agent": user_agent,
            "Content-Length": "0",
            "wtu": self.device_key,
            "Accept-Encoding": "gzip",
            "Cookie": f'NEO_SES="{self.neo_ses}"; NEO_CHK="{self.neo_chk}"'
        }
        params = {"msgpad": msgpad, "md": md}

        if method == "GET":
            response = session.get(webtoon_url, params=params, headers=headers)
        elif method == "POST":
            url_with_params = f"{webtoon_url}&msgpad={params['msgpad']}&md={params['md']}"
            response = session.post(url_with_params, json=data, headers=headers)
        elif method == "PUT":
            response = session.put(webtoon_url, params=params, json=data, headers=headers)
        elif method == "DELETE":
            response = session.delete(webtoon_url, params=params, json=data, headers=headers)
        else:
            raise ValueError(f"Unsupported HTTP method: {method}")

        response_data = json.loads(response.text)

        if response_data.get("error_code") is not None:
            if response_data["error_code"] == "025":
                raise TimeLimitError(response_data["message"])
            elif response_data["error_code"] == "024":
                raise AuthError(response_data["message"])

        if return_cookies:
            return {
                "result": response_data["message"]["result"],
                "cookies": response.cookies.get_dict(),
                "headers": response.headers
            }

        if "message" not in response_data or "result" not in response_data["message"]:
            return None
            
        return response_data["message"]["result"]

    def extract_cookie(self, headers: dict, cookie_name: str) -> str:
        cookies = headers.get('set-cookie', '').split('; ')
        for cookie in cookies:
            if cookie_name in cookie:
                return cookie[cookie.find("=")+2:-1]
        return ""

    def get_static_content(self, path: str) -> bytes:
        url = urllib.parse.urljoin("https://webtoon-phinf.pstatic.net", path)
        return session.get(
            url,
            headers={"Referer": "http://m.webtoons.com/", "User-Agent": user_agent},
        ).content

    def get_coin_balance(self) -> int:
        """Obtiene el balance de monedas de la cuenta. Retorna -1 si falla."""
        try:
            result = self.coinBalance(v=1)
            if result and "balance" in result:
                return result["balance"]["amount"]
        except Exception:
            pass
        return -1

    def __getattr__(self, name: str) -> WebtoonApiCall:
        return WebtoonApiCall(self, name)


class PrintRedirector:
    def __init__(self, textbox, root):
        self.textbox = textbox
        self.root = root

    def write(self, text):
        if not text: return
        self.textbox.configure(state="normal")
        
        if text.startswith("\r"):
            try:
                self.textbox.delete("end-2c linestart", "end-1c")
                self.textbox.insert(tk.END, text[1:])
            except tk.TclError:
                self.textbox.insert(tk.END, text[1:])
        else:
            self.textbox.insert(tk.END, text)
            
        self.textbox.see(tk.END)
        self.textbox.configure(state="disabled")
        self.root.update_idletasks()

    def flush(self):
        pass


class WebtoonDownloaderApp(ctk.CTk):
    def __init__(self):
        super().__init__()
        self.title("Webtoon Downloader")
        self.geometry("850x700")
        self.api = WebtoonApi()
        self.is_logged_in = False
        self.episodes_data = []
        self.checkboxes = {}
        self.ads_used = 0
        self.dailypass_used = 0
        self.rights_map = {}
        self.coin_balance = -1
        self.current_series_name = "Unknown_Series"
        
        self.lang = "es"
        
        self._build_language_selector()

    def t(self, key, *args):
        text = T[self.lang].get(key, key)
        if args:
            return text.format(*args)
        return text

    def _build_language_selector(self):
        self.lang_frame = ctk.CTkFrame(self)
        self.lang_frame.place(relx=0.5, rely=0.5, anchor=tk.CENTER)
        
        ctk.CTkLabel(self.lang_frame, text=T["es"]["lang_sel"], font=("Arial", 16, "bold")).pack(pady=20, padx=40)
        
        btn_frame = ctk.CTkFrame(self.lang_frame, fg_color="transparent")
        btn_frame.pack(pady=10)
        
        ctk.CTkButton(btn_frame, text="Español", command=lambda: self._set_language("es"), width=120).pack(side="left", padx=10)
        ctk.CTkButton(btn_frame, text="English", command=lambda: self._set_language("en"), width=120).pack(side="right", padx=10)

    def _set_language(self, chosen_lang):
        self.lang = chosen_lang
        self.lang_frame.destroy()
        self.title(self.t("app_title"))
        self._build_gui()
        
        sys.stdout = PrintRedirector(self.log_console, self)
        self.check_auto_login()

    def _build_gui(self):
        self.header_frame = ctk.CTkFrame(self)
        self.header_frame.pack(fill="x", padx=10, pady=10)

        self.status_indicator = ctk.CTkLabel(
            self.header_frame, text=self.t("not_logged"),
            font=("Arial", 14, "bold"), text_color="red"
        )
        self.status_indicator.pack(side="left", padx=10)

        self.balance_label = ctk.CTkLabel(
            self.header_frame, text="",
            font=("Arial", 13, "bold"), text_color="#FFD700"
        )
        self.balance_label.pack(side="left", padx=15)

        self.login_btn = ctk.CTkButton(self.header_frame, text=self.t("login_btn"), command=self.open_login_window)
        self.login_btn.pack(side="right", padx=10)

        self.search_frame = ctk.CTkFrame(self)
        self.search_frame.pack(fill="x", padx=10, pady=5)

        self.url_entry = ctk.CTkEntry(self.search_frame, placeholder_text=self.t("url_ph"), width=500)
        self.url_entry.pack(side="left", padx=10, pady=10, expand=True, fill="x")

        self.analyze_btn = ctk.CTkButton(self.search_frame, text=self.t("analyze"), command=self.start_analysis_thread)
        self.analyze_btn.pack(side="right", padx=10)

        self.center_frame = ctk.CTkFrame(self)
        self.center_frame.pack(fill="both", expand=True, padx=10, pady=5)

        self.episodes_scroll = ctk.CTkScrollableFrame(self.center_frame, label_text=self.t("ep_list"))
        self.episodes_scroll.pack(side="left", fill="both", expand=True, padx=(5, 5), pady=5)

        self.action_frame = ctk.CTkFrame(self.center_frame, width=250)
        self.action_frame.pack(side="right", fill="y", padx=(5, 5), pady=5)

        ctk.CTkLabel(self.action_frame, text=self.t("quick_sel"), font=("Arial", 12, "bold")).pack(pady=10)
        
        self.range_entry = ctk.CTkEntry(self.action_frame, placeholder_text=self.t("ranges"), width=200)
        self.range_entry.pack(pady=5, padx=10)
        self.range_entry.bind("<Return>", lambda event: self.apply_range_selection())

        self.select_btn = ctk.CTkButton(self.action_frame, text=self.t("select"), command=self.apply_range_selection)
        self.select_btn.pack(pady=5)
        
        self.clear_btn = ctk.CTkButton(self.action_frame, text=self.t("clear"), command=self.clear_selection, fg_color="gray")
        self.clear_btn.pack(pady=5)

        self.download_btn = ctk.CTkButton(
            self.action_frame, text=self.t("download"),
            command=self.start_download_thread,
            fg_color="green", hover_color="darkgreen", height=40
        )
        self.download_btn.pack(side="bottom", pady=20, padx=10, fill="x")

        self.log_frame = ctk.CTkFrame(self)
        self.log_frame.pack(fill="x", padx=10, pady=5)
        
        self.log_console = ctk.CTkTextbox(self.log_frame, height=150, state="normal")
        self.log_console.pack(fill="both", expand=True, padx=5, pady=5)

    def check_auto_login(self):
        if os.path.exists(CONFIG_FILE):
            try:
                with open(CONFIG_FILE, "r") as f:
                    config = json.load(f)
                
                self.api.load_session(config.get("neo_ses", ""), config.get("neo_chk", ""), config.get("device_key", ""))
                
                print(self.t("val_session"))
                account_data = self.api.getMemberInfo()
                if account_data is not None:
                    self.update_status(True, account_data["nickname"])
                else:
                    print(self.t("cookie_exp"))
                    email = config.get("email")
                    password = config.get("password")
                    if email and password:
                        self.api.login(email, password)
                        account_data = self.api.getMemberInfo()
                        if account_data:
                            self.save_config(email, password)
                            self.update_status(True, account_data["nickname"])
                        else:
                            self.update_status(False)
            except Exception as e:
                print(self.t("err_auto", str(e)))
                self.update_status(False)

    def save_config(self, email, password):
        config = {
            "email": email,
            "password": password,
            "neo_ses": self.api.neo_ses,
            "neo_chk": self.api.neo_chk,
            "device_key": self.api.device_key
        }
        with open(CONFIG_FILE, "w") as f:
            json.dump(config, f)

    def update_status(self, logged_in, username=""):
        self.is_logged_in = logged_in
        if logged_in:
            self.status_indicator.configure(text=f"🟢 {username}", text_color="green")
            self.login_btn.configure(text=self.t("change_acc"))
            print(self.t("conn_as", username))
            # Obtener balance en hilo separado para no bloquear la UI
            threading.Thread(target=self._refresh_balance, daemon=True).start()
        else:
            self.status_indicator.configure(text=self.t("not_logged"), text_color="red")
            self.login_btn.configure(text=self.t("login_btn"))
            self.balance_label.configure(text="")

    def _refresh_balance(self):
        balance = self.api.get_coin_balance()
        self.coin_balance = balance
        if balance >= 0:
            self.after(0, lambda: self.balance_label.configure(text=self.t("coins", balance)))
        else:
            self.after(0, lambda: self.balance_label.configure(text=""))

    def open_login_window(self):
        login_win = ctk.CTkToplevel(self)
        login_win.title(self.t("login_btn"))
        login_win.geometry("300x250")
        login_win.transient(self)
        login_win.grab_set()

        ctk.CTkLabel(login_win, text=self.t("email")).pack(pady=(10, 0))
        email_entry = ctk.CTkEntry(login_win, width=250)
        email_entry.pack(pady=5)

        ctk.CTkLabel(login_win, text=self.t("pass")).pack(pady=(10, 0))
        pass_entry = ctk.CTkEntry(login_win, width=250, show="*")
        pass_entry.pack(pady=5)

        def do_login():
            email = email_entry.get()
            password = pass_entry.get()
            if not email or not password:
                messagebox.showerror("Error", self.t("err_empty"))
                return
            
            login_win.destroy()
            threading.Thread(target=self._process_login, args=(email, password), daemon=True).start()

        ctk.CTkButton(login_win, text=self.t("enter"), command=do_login).pack(pady=20)

    def _process_login(self, email, password):
        print(self.t("init_login"))
        try:
            self.api.login(email, password)
            account_data = self.api.getMemberInfo()
            if account_data:
                self.save_config(email, password)
                self.after(0, lambda: self.update_status(True, account_data["nickname"]))
            else:
                self.after(0, lambda: messagebox.showerror("Error", self.t("err_invalid")))
        except Exception as e:
            print(self.t("err_login"))
            self.after(0, lambda: messagebox.showerror("Error", self.t("err_login")))

    def get_series_no(self):
        val = self.url_entry.get().strip()
        if not val:
            return None
        if val.isdigit():
            return int(val)
        match = re.search(r'title_no=(\d+)', val)
        if match:
            return int(match.group(1))
        return None

    def start_analysis_thread(self):
        if not self.is_logged_in:
            messagebox.showwarning("Atención / Warning", self.t("err_no_login"))
            return
        series_no = self.get_series_no()
        if not series_no:
            messagebox.showwarning("Error", self.t("err_url"))
            return

        self.analyze_btn.configure(state="disabled")
        print(self.t("analyzing"))
        
        for widget in self.episodes_scroll.winfo_children():
            widget.destroy()
        self.checkboxes.clear()

        threading.Thread(target=self._process_analysis, args=(series_no,), daemon=True).start()

    def _process_analysis(self, series_no):
        try:
            title_info = self.api.titleInfo(titleNo=series_no)
            if not title_info:
                print(self.t("no_series"))
                self.after(0, lambda: self.analyze_btn.configure(state="normal"))
                return
                
            total_episodes = title_info["titleInfo"]["totalEpisodeCount"]
            raw_title_name = title_info["titleInfo"].get("titleName", title_info["titleInfo"].get("title", f"ID {series_no}"))
            
            self.current_series_name = "".join([c for c in raw_title_name if c.isalpha() or c.isdigit() or c in (' ', '-', '_')]).rstrip()
            
            print(self.t("series_info", raw_title_name, total_episodes))

            rights_data = self.api.productRightList(titleNo=series_no, v=1)
            right_list = rights_data.get("rightList", []) if rights_data else []
            self.rights_map = {r["episodeNo"]: r for r in right_list}

            title_data = self.api.episodeList(v=6, titleNo=series_no, startIndex=0, pageSize=total_episodes)
            public_episodes = title_data["episodeList"]["episode"]
            
            known_episodes_nos = {ep["episodeNo"] for ep in public_episodes}
            premium_episodes = []
            
            missing_eps = [r["episodeNo"] for r in right_list if r["episodeNo"] not in known_episodes_nos]
            
            if missing_eps:
                print(self.t("detect_fast", len(missing_eps)))
                for ep_no in missing_eps:
                    prod_data = self.api.getProduct(v=3, titleNo=series_no, episodeNo=ep_no)
                    
                    allows_ad = False
                    price = 0
                    
                    if prod_data and "product" in prod_data:
                        ep_title = prod_data["product"].get("episodeTitle", f"Capítulo {ep_no}")
                        
                        sale_units = prod_data["product"].get("saleUnitList", [])
                        for unit in sale_units:
                            if unit.get("saleUnitType") == "REWARD_AD":
                                allows_ad = True
                            elif unit.get("saleUnitType") in ("PREVIEW", "COMPLETE"):
                                price = unit.get("policyPrice", price)
                                
                        premium_episodes.append({
                            "episodeNo": ep_no,
                            "episodeTitle": ep_title,
                            "productInfo": True,
                            "allows_ad": allows_ad,
                            "price": price
                        })

            self.episodes_data = sorted(public_episodes + premium_episodes, key=lambda e: e["episodeNo"], reverse=True)
            self.after(0, self._render_checkboxes)
            
        except Exception as e:
            print(f"Error: {e}")
        finally:
            self.after(0, lambda: self.analyze_btn.configure(state="normal"))

    def _render_checkboxes(self):
        for ep in self.episodes_data:
            ep_no = ep["episodeNo"]
            title = ep["episodeTitle"]
            
            right_info = self.rights_map.get(ep_no, {})
            has_right = right_info.get("hasRight", False)
            is_infinite = right_info.get("infinite", False)
            
            if "productInfo" not in ep:
                status_text = self.t("status_free")
                color = "white"
            elif has_right:
                if is_infinite:
                    status_text = self.t("status_bought")
                    color = "#00FF00"
                else:
                    status_text = self.t("status_temp")
                    color = "lightgreen"
            else:
                allows_ad = ep.get("allows_ad", False)
                price = ep.get("price", 0)
                
                if allows_ad and price > 0:
                    status_text = self.t("status_ads", price)
                    color = "yellow"
                elif allows_ad or price == 0:
                    status_text = self.t("status_ads_only")
                    color = "#00FFFF" # Cian para diferenciar que NO usa monedas
                else:
                    status_text = self.t("status_paid", price)
                    color = "orange"

            display_text = f"Cap {ep_no} - {title} {status_text}"
            
            var = ctk.BooleanVar(value=False)
            chk = ctk.CTkCheckBox(self.episodes_scroll, text=display_text, variable=var, text_color=color)
            chk.pack(anchor="w", pady=2, padx=5)
            self.checkboxes[ep_no] = {"var": var, "ep_data": ep}
        
        print(self.t("analysis_ok"))

    def apply_range_selection(self):
        range_str = self.range_entry.get().strip()
        if not range_str:
            return
            
        selected_numbers = set()
        parts = range_str.split(',')
        
        try:
            for part in parts:
                part = part.strip()
                if '-' in part:
                    start, end = map(int, part.split('-'))
                    selected_numbers.update(range(start, end + 1))
                else:
                    selected_numbers.add(int(part))
                    
            for ep_no, chk_dict in self.checkboxes.items():
                if ep_no in selected_numbers:
                    chk_dict["var"].set(True)
            print(self.t("sel_range", sorted(list(selected_numbers))))
        except ValueError:
            messagebox.showwarning("Error", self.t("err_fmt"))

    def clear_selection(self):
        for chk_dict in self.checkboxes.values():
            chk_dict["var"].set(False)
        self.range_entry.delete(0, 'end')

    def start_download_thread(self):
        series_no = self.get_series_no()
        episodes_to_download = []
        
        for ep_no, chk_dict in sorted(self.checkboxes.items()):
            if chk_dict["var"].get():
                episodes_to_download.append(chk_dict["ep_data"])

        if not episodes_to_download:
            messagebox.showwarning("Aviso / Warning", self.t("err_no_sel"))
            return

        self.download_btn.configure(state="disabled")
        threading.Thread(target=self._process_download, args=(series_no, episodes_to_download), daemon=True).start()

    def _process_download(self, series_no, episodes_to_download):
        print(self.t("init_dl"))
        
        series_dir = os.path.join(DOWNLOAD_DIR, self.current_series_name)
        os.makedirs(series_dir, exist_ok=True)

        self.coin_balance = self.api.get_coin_balance()
        
        for ep in episodes_to_download:
            ep_no = ep["episodeNo"]
            right_info = self.rights_map.get(ep_no, {})
            
            try:
                self.ads_used, self.dailypass_used, self.coin_balance = self._download_single_episode(
                    series_no=series_no,
                    episode=ep,
                    right_info=right_info,
                    ads_used=self.ads_used,
                    dailypass_used=self.dailypass_used,
                    coin_balance=self.coin_balance,
                    series_dir=series_dir
                )
            except Exception as e:
                print(self.t("fatal_err", ep_no, str(e)))
            
            time.sleep(1.5)

        self.after(0, lambda: self.balance_label.configure(text=self.t("coins", self.coin_balance) if self.coin_balance >= 0 else ""))
        print(self.t("dl_done"))
        self.after(0, lambda: self.download_btn.configure(state="normal"))
        
        print(self.t("refreshing_list"))
        self.after(1000, self.start_analysis_thread)

    def _download_single_episode(self, series_no, episode, right_info, ads_used, dailypass_used, coin_balance, series_dir, BUY_EPISODES=True):
        episode_number = episode["episodeNo"]
        print(self.t("eval_cap", episode_number))
        
        is_free = "productInfo" not in episode
        has_right = right_info.get("hasRight", False)
        is_infinite = right_info.get("infinite", False)

        if not is_free and not has_right:
            live_right = self.api.productRight(titleNo=series_no, episodeNo=episode_number)
            if live_right:
                has_right = live_right.get("hasRight", False)
                is_infinite = live_right.get("infinite", False)
        
        if is_free or has_right:
            if is_free:
                print(self.t("free_pub"))
            elif is_infinite:
                print(self.t("cap_bought_inf"))
            else:
                print(self.t("cap_unl_temp"))
        else:
            print(self.t("chk_opts"))
            product_data = self.api.getProduct(v=3, titleNo=series_no, episodeNo=episode_number)
            
            allows_ad = False
            allows_daily_pass = False
            price = 3
            product_id = f"linewebtoon-WEBTOON-{series_no}-{episode_number}"
            sale_type_str = "complete"
            
            if product_data and "product" in product_data:
                sale_units = product_data["product"].get("saleUnitList", [])
                for unit in sale_units:
                    unit_type = unit.get("saleUnitType", "")
                    if unit_type == "REWARD_AD":
                        allows_ad = True
                    elif unit_type == "DAILY_PASS":
                        allows_daily_pass = True
                    elif unit_type in ("PREVIEW", "COMPLETE"):
                        price = unit.get("policyPrice", price)
                        sale_type_str = unit_type.lower()

            if allows_ad and ads_used < 100:
                print(self.t("ads_ok"))
                time.sleep(32)
                self.api.getImageSecureToken(v=1)
                self.api.buyProduct(
                    method="POST",
                    productId=product_id,
                    productSaleUnitId=f"{product_id}-reward_ad-1",
                    price=0,
                )
                ads_used += 1
                print(self.t("ad_claimed"))

            elif allows_daily_pass and dailypass_used < 1:
                self.api.buyProduct(
                    method="POST",
                    productId=product_id,
                    productSaleUnitId=f"{product_id}-complete_daily_pass-1",
                    price=0,
                )
                dailypass_used += 1
                print(self.t("pass_claimed"))

            elif BUY_EPISODES:
                if coin_balance >= 0 and coin_balance < price:
                    print(self.t("no_coins", coin_balance, price))
                    return ads_used, dailypass_used, coin_balance

                self.api.buyProduct(
                    method="POST",
                    productId=product_id,
                    productSaleUnitId=f"{product_id}-{sale_type_str}-1",
                    price=price,
                )
                coin_balance = max(0, coin_balance - price)
                _bal = coin_balance
                self.after(0, lambda b=_bal: self.balance_label.configure(text=self.t("coins", b)))
                print(self.t("bought"))

            else:
                print(self.t("prem_closed"))
                return ads_used, dailypass_used, coin_balance

        response = self.api.episodeInfoWithLogin(v=4, titleNo=series_no, episodeNo=episode_number)

        if response is None or "episodeInfo" not in response:
            print(self.t("err_imgs", episode_number))
            return ads_used, dailypass_used, coin_balance

        episode_title_real = response["episodeInfo"]["episodeTitle"]
        safe_folder_name = "".join([c for c in episode_title_real if c.isalpha() or c.isdigit() or c in (' ', '-', '_')]).rstrip()
        
        episode_path = os.path.join(series_dir, safe_folder_name)
        
        if os.path.isdir(episode_path):
            print(self.t("skip_exist", self.current_series_name, safe_folder_name))
            return ads_used, dailypass_used, coin_balance

        os.makedirs(episode_path, exist_ok=True)
        print(self.t("saving_in", self.current_series_name, safe_folder_name))
        
        raw_images = response["episodeInfo"]["imageInfo"]
        valid_images = [img for img in raw_images if "capture_warning" not in img["url"]]
        total_images = len(valid_images)
        
        for index, image in enumerate(valid_images):
            progress_str = "\r" + self.t("dl_prog", index + 1, total_images)
            sys.stdout.write(progress_str)
            sys.stdout.flush()
            
            img_index = index + 1
            img_filename = f"{img_index:03d}.jpg"
            
            with open(os.path.join(episode_path, img_filename), "wb") as file:
                file.write(self.api.get_static_content(image["url"]))
                
        sys.stdout.write("\n")
        print(self.t("cap_done"))
        return ads_used, dailypass_used, coin_balance

if __name__ == "__main__":
    app = WebtoonDownloaderApp()
    app.mainloop()