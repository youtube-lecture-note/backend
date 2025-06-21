from flask import Flask, request, jsonify
from youtube_transcript_api import YouTubeTranscriptApi, TranscriptsDisabled, NoTranscriptFound, VideoUnavailable
from youtube_transcript_api.proxies import WebshareProxyConfig
import requests
from bs4 import BeautifulSoup
import time


app = Flask(__name__)

@app.route('/transcript')
def get_youtube_transcript():
    try:
        video_id = request.args.get('video_id')
        ytt_api = YouTubeTranscriptApi(
            proxy_config=WebshareProxyConfig(
                proxy_username="username",
                proxy_password="password",
            )
        )
        transcript = YouTubeTranscriptApi.get_transcript(video_id, languages=['ko'], proxies={"http": "http://uwhjekjl-rotate:sg2j8g7mpsjc@p.webshare.io:80", "https": "http://uwhjekjl-rotate:sg2j8g7mpsjc@p.webshare.io:80"})
        # 사용 가능한 자막 목록 확인
        # transcript_list = ytt_api.list(video_id)
        # transcript = transcript_list.find_transcript(['ko', 'en'])
        
        print("1",transcript)
        fetched_transcript = transcript.fetch()
        print(fetched_transcript)
        return jsonify({
            'success': True,
            'transcript': fetched_transcript.to_raw_data()})
        
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500


@app.route('/youtube-title', methods=['GET'])
def get_video_title():
    video_id = request.args.get('id')
    if not video_id:
        return jsonify({'error': 'video_id is required'}), 400

    url = f"https://www.youtube.com/watch?v={video_id}"
    headers = {
        'User-Agent': 'Mozilla/5.0'
    }

    try:
        response = requests.get(url, headers=headers)
        if response.status_code != 200:
            return jsonify({'error': 'Failed to fetch video page'}), 500

        soup = BeautifulSoup(response.text, 'html.parser')
        title_tag = soup.find('title')
        if not title_tag:
            return jsonify({'error': 'Could not find title'}), 500

        # " - YouTube" 제거
        title = title_tag.text.replace(" - YouTube", "").strip()
        return jsonify({'title': title})

    except Exception as e:
        return jsonify({'error': str(e)}), 500


if __name__ == '__main__':
    # Make sure to set host='0.0.0.0' if you want to access it from
    # your Java application running on the same machine but outside a Docker container.
    # If both are in Docker and on the same network, 'localhost' or service name might work.
    app.run(host='0.0.0.0', port=3456, debug=False)